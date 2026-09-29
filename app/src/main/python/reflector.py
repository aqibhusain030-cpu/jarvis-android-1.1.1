"""Post-goal reflection — emits a one-line lesson."""


def reflect(goal_title: str, succeeded: bool, failures: int) -> str:
    if succeeded and failures == 0:
        return f"Goal '{goal_title}' succeeded on first attempt."
    if succeeded:
        return f"Goal '{goal_title}' succeeded after {failures} retries."
    return f"Goal '{goal_title}' failed. Review tool availability and confirmations."
