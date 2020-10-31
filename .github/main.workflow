workflow "build" {
  resolves = ["GitHub Action for Maven & JDK 14"]
  on = "push"
}

workflow "pull request" {
  resolves = ["GitHub Action for Maven & JDK 14"]
  on = "pull_request"
}

action "GitHub Action for Maven & JDK 14" {
  uses = "xlui/action-maven-cli/jdk14@master"
  args = "test"
}
