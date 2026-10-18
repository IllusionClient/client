plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.8.9-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "forge")
    }
}
