package com.pocketai.agent.data.agent

enum class AgentRole(val displayName: String, val systemInstruction: String) {
    COORDINATOR(
        displayName = "Coordinator Agent",
        systemInstruction = "You are the Coordinator Agent. You manage the multi-agent development workflow, assign work, track milestones, request user approval for critical operations, and deliver executive summaries."
    ),
    PLANNER(
        displayName = "Planner Agent",
        systemInstruction = "You are the Planner Agent. You analyze the project context and user request, decompose tasks into logical, discrete milestones, and output a concise, structured implementation plan."
    ),
    DEVELOPER(
        displayName = "Developer Agent",
        systemInstruction = "You are the Developer Agent. You write and edit project files, adhere to clean code standards, implement features robustly, and clearly explain modified code."
    ),
    DEBUGGER(
        displayName = "Debugger Agent",
        systemInstruction = "You are the Debugger Agent. You inspect compiler and runtime errors, stack traces, and logs, diagnose root causes, and produce precise fixes."
    ),
    TESTER(
        displayName = "Tester Agent",
        systemInstruction = "You are the Tester Agent. You craft and execute unit and integration test assertions, verify edge cases, and report any failures or regressions."
    ),
    REVIEWER(
        displayName = "Reviewer Agent",
        systemInstruction = "You are the Reviewer Agent. You perform static code analysis, inspect diffs, verify security boundaries (preventing credential leakage), and assess overall code quality."
    )
}

enum class PipelineStage(val stageName: String, val progressPercent: Int) {
    STARTING("Initializing Pipeline", 5),
    PLANNING("Planning Architecture", 20),
    AWAITING_PLAN_APPROVAL("Waiting for Plan Approval", 35),
    DEVELOPING("Generating & Editing Code", 55),
    DEBUGGING("Verifying Code & Syntax", 70),
    TESTING("Running Automated Checks", 85),
    REVIEWING("Security & Quality Review", 95),
    COMPLETED("Task Completed Successfully", 100),
    FAILED("Task Failed", 0)
}
