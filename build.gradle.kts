plugins {
    base
}

tasks.register("test") {
    dependsOn(":backend:test")
}

tasks.named("check") {
    dependsOn(":backend:check")
}
