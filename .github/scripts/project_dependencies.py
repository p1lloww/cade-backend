#!/usr/bin/env python3
"""Marks which Project #4 cards have all their dependencies done.

Usage: project_dependencies.py [--dry-run]

Reads every card of the Project (both repos), takes the codes in "Depende de" and fills:
  - "Dependências": Liberada (all done), Bloqueada (something pending) or Sem dependências;
  - "Aguardando": the codes still pending, e.g. "ACE-02, GES-01".
A dependency counts as done when its card is in "Concluído" or its issue is closed.
Only these two fields are written, and only when the value changes.

Needs GH_TOKEN with the "project" scope: the default GITHUB_TOKEN cannot reach a user Project.
"""

import json
import re
import subprocess
import sys

OWNER = "p1lloww"
PROJECT_NUMBER = 4

DONE_STATUS = "Concluído"
FREE, BLOCKED, NO_DEPS = "Liberada", "Bloqueada", "Sem dependências"

CODE = re.compile(r"[A-Z]{3}-\d{2}")
TITLE_CODE = re.compile(r"^\[([A-Z]{3}-\d{2})\]")

ITEMS_QUERY = """
query($owner: String!, $number: Int!, $after: String) {
  user(login: $owner) {
    projectV2(number: $number) {
      id
      fields(first: 50) {
        nodes {
          ... on ProjectV2FieldCommon { id name }
          ... on ProjectV2SingleSelectField { options { id name } }
        }
      }
      items(first: 100, after: $after) {
        pageInfo { hasNextPage endCursor }
        nodes {
          id
          content { ... on Issue { title state } }
          status: fieldValueByName(name: "Status") {
            ... on ProjectV2ItemFieldSingleSelectValue { name }
          }
          dependsOn: fieldValueByName(name: "Depende de") {
            ... on ProjectV2ItemFieldTextValue { text }
          }
          dependencies: fieldValueByName(name: "Dependências") {
            ... on ProjectV2ItemFieldSingleSelectValue { name }
          }
          waitingFor: fieldValueByName(name: "Aguardando") {
            ... on ProjectV2ItemFieldTextValue { text }
          }
        }
      }
    }
  }
}"""

SET_VALUE = """
mutation($project: ID!, $item: ID!, $field: ID!, $value: ProjectV2FieldValue!) {
  updateProjectV2ItemFieldValue(
    input: {projectId: $project, itemId: $item, fieldId: $field, value: $value}
  ) { clientMutationId }
}"""

CLEAR_VALUE = """
mutation($project: ID!, $item: ID!, $field: ID!) {
  clearProjectV2ItemFieldValue(
    input: {projectId: $project, itemId: $item, fieldId: $field}
  ) { clientMutationId }
}"""


def graphql(query, **variables):
    payload = json.dumps({"query": query, "variables": variables})
    result = subprocess.run(["gh", "api", "graphql", "--input", "-"], input=payload,
                            capture_output=True, text=True)
    data = json.loads(result.stdout or "{}")
    if result.returncode != 0 or "errors" in data:
        raise RuntimeError(data.get("errors") or result.stderr)
    return data["data"]


def load_project():
    """Project id, fields by name and the cards that are issues."""
    items, after = [], None
    while True:
        project = graphql(ITEMS_QUERY, owner=OWNER, number=PROJECT_NUMBER,
                          after=after)["user"]["projectV2"]
        page = project["items"]
        items += [i for i in page["nodes"] if (i["content"] or {}).get("title")]
        if not page["pageInfo"]["hasNextPage"]:
            break
        after = page["pageInfo"]["endCursor"]
    fields = {f["name"]: f for f in project["fields"]["nodes"] if f}
    return project["id"], fields, items


def value(item, key, attr):
    return (item[key] or {}).get(attr) or ""


def main():
    dry_run = "--dry-run" in sys.argv
    project_id, fields, items = load_project()

    done = {}
    for item in items:
        match = TITLE_CODE.match(item["content"]["title"])
        if match:
            done[match.group(1)] = (value(item, "status", "name") == DONE_STATUS
                                    or item["content"]["state"] == "CLOSED")

    options = {o["name"]: o["id"] for o in fields["Dependências"]["options"]}
    changed = 0
    for item in items:
        label = item["content"]["title"].split("]")[0].lstrip("[")
        deps = CODE.findall(value(item, "dependsOn", "text"))
        unknown = [d for d in deps if d not in done]
        if unknown:
            print(f"aviso: {label} depende de código inexistente {unknown}")
        pending = [d for d in deps if not done.get(d)]
        state = NO_DEPS if not deps else (BLOCKED if pending else FREE)
        waiting = ", ".join(pending)

        if (state, waiting) == (value(item, "dependencies", "name"), value(item, "waitingFor", "text")):
            continue
        changed += 1
        print(f"{label} -> {state}" + (f" (aguardando {waiting})" if waiting else ""))
        if dry_run:
            continue
        ids = {"project": project_id, "item": item["id"]}
        graphql(SET_VALUE, **ids, field=fields["Dependências"]["id"],
                value={"singleSelectOptionId": options[state]})
        if waiting:
            graphql(SET_VALUE, **ids, field=fields["Aguardando"]["id"], value={"text": waiting})
        else:
            graphql(CLEAR_VALUE, **ids, field=fields["Aguardando"]["id"])

    print(f"{changed} card(s) {'a atualizar' if dry_run else 'atualizado(s)'} de {len(items)}")


if __name__ == "__main__":
    main()
