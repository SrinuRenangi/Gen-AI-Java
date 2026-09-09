package com.genai.langchain4j.extraction;

import java.util.List;

/**
 * Structured domain entity representing an extracted patient triage report.
 */
public record ClinicalReport(
    @Description("Full legal name of patient")
    String patientName,

    @Description("Patient age in full calendar years")
    int age,

    @Description("Measured systolic and diastolic blood pressure, e.g., '120/80'")
    String bloodPressure,

    @Description("Heart rate in beats per minute")
    int heartRateBpm,

    @Description("List of clinical symptoms identified in clinical transcript")
    List<String> detectedSymptoms,

    @Description("Clinical triage urgency level: IMMEDIATE, URGENT, ROUTINE")
    TriageLevel triageUrgency,

    @Description("Verbatim sentence quote from the doctor's transcript confirming symptoms and vitals")
    String verbatimCitation
) {
    public enum TriageLevel { IMMEDIATE, URGENT, ROUTINE }
}
