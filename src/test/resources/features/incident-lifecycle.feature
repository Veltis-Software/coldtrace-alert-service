@alerts
Feature: Incident lifecycle from a threshold breach to its closure
  As an operations manager
  I want each deviation to become a traceable incident
  So that the team responds and keeps evidence of every action

  Background:
    Given an operator "David" and a colleague "Renso" in organization 1
    And a CRITICAL threshold breach of 12 degrees for asset "Cold room" with a safe range of 2 to 8 degrees

  @US06
  Scenario: A threshold breach opens one incident with its notification
    Then one OPEN incident and one notification exist for organization 1

  @US33
  Scenario: The operator reviews the alert detail
    When "David" opens the alert detail
    Then the detail shows asset "Cold room", a reading of 12 degrees and a maximum of 8 degrees

  @US08
  Scenario: The operator acknowledges the alert and assigns a colleague
    When "David" acknowledges the alert and assigns it to "Renso"
    Then the incident state is ACKNOWLEDGED and the assignee is "Renso"

  @US08
  Scenario: A user without update permission cannot acknowledge the alert
    When a reader acknowledges the alert
    Then the request is rejected as forbidden and the incident remains OPEN

  @US10 @US11
  Scenario: The operator records a corrective action and resolves the incident
    When "David" records the corrective action "Door seal replaced"
    And "David" resolves the incident with "Cooling restored and inspected"
    Then the incident state is RESOLVED
    And the corrective actions are authored by "David"

  @US13 @US39 @US41
  Scenario: The quality manager filters the incident history
    When "David" resolves the incident with "Cooling restored and inspected"
    And the incident history is filtered by state RESOLVED and severity CRITICAL
    Then the history contains 1 incident
    And filtering the history by state OPEN returns 0 incidents
