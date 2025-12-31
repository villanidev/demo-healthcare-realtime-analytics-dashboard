import http from "k6/http";
import { check, sleep } from "k6";

// Base URL of the app; override when running against Docker, etc.
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

// Simple defaults; you can override via env vars
const ORG_ID = __ENV.ORG_ID || "1";
const CLINIC_ID = __ENV.CLINIC_ID || "1";
const PATIENT_ID = __ENV.PATIENT_ID || "1";

// Load profile parameters (all overrideable via env vars)
// How many virtual users to ramp up to for the steady-state phase
const RAMP_TARGET_VUS = Number(__ENV.RAMP_TARGET_VUS || "40");
// How long to hold the target load once reached (in minutes)
const RAMP_HOLD_MINUTES = Number(__ENV.RAMP_HOLD_MINUTES || "3");

// Optional spike on top of the base load; 0 disables the spike
const SPIKE_VUS = Number(__ENV.SPIKE_VUS || "0"); // default disabled
const SPIKE_DURATION = __ENV.SPIKE_DURATION || "2m";
const SPIKE_START = __ENV.SPIKE_START || "7m";

// Number of long-lived SSE clients
const SSE_CLIENTS = Number(__ENV.SSE_CLIENTS || "15");

// Capture the logical start time of the test so we can later
// query appointments scheduled since this moment via the stats API.
const TEST_START_ISO = new Date().toISOString();

export const options = {
  scenarios: {
    schedule_ramp_load: {
      executor: "ramping-vus",
      exec: "scheduleFlow",
      startVUs: 0,
      stages: [
        { duration: "2m", target: RAMP_TARGET_VUS }, // ramp up
        { duration: `${RAMP_HOLD_MINUTES}m`, target: RAMP_TARGET_VUS }, // hold
        { duration: "1m", target: 0 }, // ramp down
      ],
      gracefulRampDown: "30s",
    },
    schedule_spike_load: {
      executor: "constant-vus",
      exec: "scheduleFlow",
      vus: SPIKE_VUS, // if 0, this scenario effectively does nothing
      duration: SPIKE_DURATION,
      startTime: SPIKE_START,
      gracefulStop: "30s",
    },
    sse_clients: {
      executor: "constant-vus",
      exec: "sseClient",
      vus: SSE_CLIENTS,
      duration: "10m",
      gracefulStop: "10s",
      startTime: "0s",
    },
    // After the scheduling load is done, we run a single-VU
    // scenario that polls the stats API until all expected
    // appointments are visible (or a max number of polls), and
    // then completes them. EXPECTED_SCHEDULED_COUNT is optional
    // and can be provided via env if you want a strict assertion.
    wait_and_complete_all: {
      executor: "per-vu-iterations",
      exec: "waitAndCompleteAllScheduled",
      vus: Number(__ENV.WAIT_COMPLETE_VUS || "1"),
      iterations: 1,
      // Start after ramp + hold + ramp-down and spike by default.
      // You can override via WAIT_COMPLETE_START if needed.
      startTime: __ENV.WAIT_COMPLETE_START || "10m",
      gracefulStop: "2m",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"], // <5% errors
    http_req_duration: ["p(95)<2000"], // 95% under 2s (tuned for small container)
  },
};

export function scheduleFlow() {
  // Schedule an appointment
  const schedulePayload = JSON.stringify({
    organizationId: Number(ORG_ID),
    clinicId: Number(CLINIC_ID),
    patientId: Number(PATIENT_ID),
    modality: Math.random() < 0.5 ? "VIRTUAL" : "IN_PERSON",
    // let server default scheduledAtIso to now, omit for simplicity
  });

  const scheduleRes = http.post(
    `${BASE_URL}/api/appointments`,
    schedulePayload,
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "schedule_appointment" },
    }
  );

  // In async queue mode the API returns 202 on success; in
  // sync mode we still get 200. We treat both as success, and
  // consider 503 (queue full) as an expected backpressure
  // signal rather than a hard error.
  const ok = scheduleRes.status === 200 || scheduleRes.status === 202;
  const backpressure = scheduleRes.status === 503;

  check(scheduleRes, {
    "schedule accepted (200/202)": (r) => ok,
  });

  if (!ok && !backpressure) {
    // unexpected failure; pause briefly
    // brief pause before next iteration
    sleep(1);
    return;
  }
  // Short pause before next schedule
  sleep(1);
}

// This helper scenario is meant to run after the main scheduling
// load. It queries the stats endpoint from TEST_START_ISO until
// it sees all expected appointments (if EXPECTED_SCHEDULED_COUNT
// is provided), then completes each appointment ID returned. When
// multiple VUs are used for this scenario, each VU processes only
// its own partition of the ID list based on __VU so we avoid
// every VU completing all IDs.
export function waitAndCompleteAllScheduled() {
  const expectedEnv = __ENV.EXPECTED_SCHEDULED_COUNT;
  const expected = expectedEnv ? Number(expectedEnv) : null;
  const maxPolls = Number(__ENV.STATS_MAX_POLLS || "60");
  const pollIntervalSeconds = Number(__ENV.STATS_POLL_INTERVAL_SECONDS || "5");

  let polls = 0;
  let lastCount = 0;
  let appointmentIds = [];

  while (polls < maxPolls) {
    const url =
      `${BASE_URL}/api/appointments/stats/by-scheduled-at` +
      `?organizationId=${ORG_ID}` +
      `&clinicId=${CLINIC_ID}` +
      `&fromScheduledAtIso=${encodeURIComponent(TEST_START_ISO)}`;

    const res = http.get(url, { tags: { name: "appointment_stats" } });

    const ok = res.status === 200;
    check(res, {
      "stats status is 200": (r) => ok,
    });

    if (ok) {
      const body = res.json();
      lastCount = body.count || 0;
      appointmentIds = body.appointmentIds || [];

      if (expected === null || lastCount >= expected) {
        break;
      }
    }

    polls += 1;
    sleep(pollIntervalSeconds);
  }

  if (expected !== null) {
    check(null, {
      "all expected appointments visible in stats": () => lastCount >= expected,
    });
  }

  // Partition the ID list across VUs so each VU handles a distinct
  // slice. We rely on WAIT_COMPLETE_VUS matching the VU count for
  // this scenario.
  const totalPartitions = Number(__ENV.WAIT_COMPLETE_VUS || "1");
  const vuIndex = (__VU || 1) - 1; // __VU is 1-based in k6

  const chunkSize = Math.ceil(appointmentIds.length / totalPartitions) || 0;
  const start = vuIndex * chunkSize;
  const end = Math.min(start + chunkSize, appointmentIds.length);
  const idsForThisVu = appointmentIds.slice(start, end);

  // Now drive completion for this VU's subset of IDs.
  for (const id of idsForThisVu) {
    const completePayload = JSON.stringify({});
    const res = http.patch(
      `${BASE_URL}/api/appointments/${id}`,
      completePayload,
      {
        headers: { "Content-Type": "application/json" },
        tags: { name: "complete_from_stats" },
      }
    );

    check(res, {
      "complete status is 200 or 404": (r) =>
        r.status === 200 || r.status === 404,
    });

    // Small pacing to avoid hammering the DB in a tight loop.
    sleep(0.1);
  }
}

// For now we keep completes synchronous but model them as a separate
// scenario that runs at a lower rate and only for a subset of
// appointments, approximating "schedule now, complete later". In a
// real system we'd typically drive this from a list of existing
// appointments.
export function completeFlow() {
  // For the POC, we still complete immediately after scheduling within
  // this iteration, but we run fewer complete VUs than schedule VUs to
  // reduce pressure and better match real-world behaviour.

  const schedulePayload = JSON.stringify({
    organizationId: Number(ORG_ID),
    clinicId: Number(CLINIC_ID),
    patientId: Number(PATIENT_ID),
    modality: Math.random() < 0.5 ? "VIRTUAL" : "IN_PERSON",
  });

  const scheduleRes = http.post(
    `${BASE_URL}/api/appointments`,
    schedulePayload,
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "schedule_for_complete" },
    }
  );

  if (scheduleRes.status !== 200) {
    sleep(1);
    return;
  }

  const appointmentId = scheduleRes.json("id");

  // Simulate a delay between schedule and complete. We can't sleep
  // minutes in a short test, but a small random delay breaks the
  // immediate back-to-back pattern.
  sleep(0.5 + Math.random());

  const completePayload = JSON.stringify({});
  const completeRes = http.patch(
    `${BASE_URL}/api/appointments/${appointmentId}`,
    completePayload,
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "complete_appointment" },
    }
  );

  check(completeRes, {
    "complete status is 200": (r) => r.status === 200,
  });

  sleep(1);
}

// Simulate long-lived SSE clients by keeping HTTP connections open
// against the /api/analytics/stream endpoint. Each VU acts as one
// SSE client for the duration of the scenario.
export function sseClient() {
  const url = `${BASE_URL}/api/analytics/stream?organizationId=${ORG_ID}&clinicId=${CLINIC_ID}`;

  const res = http.get(url, {
    tags: { name: "sse_stream" },
    timeout: "5m", // keep connection open as long as possible
  });

  // We expect a 200 and a long-lived connection; if the server closes
  // early, this check will still tell us it was at least established.
  check(res, {
    // Treat any 2xx/3xx status as a successful connection. Network-level
    // errors will have status 0 and still be counted as failures, but
    // won't break the test logic.
    "sse connected": (r) => r.status >= 200 && r.status < 400,
  });

  // Small pause before reconnecting if the request returned.
  sleep(1);
}
