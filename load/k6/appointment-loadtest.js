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

  check(scheduleRes, {
    "schedule status is 200": (r) => r.status === 200,
  });

  if (scheduleRes.status !== 200) {
    // brief pause before next iteration
    sleep(1);
    return;
  }
  // Short pause before next schedule
  sleep(1);
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
