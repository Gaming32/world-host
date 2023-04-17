plugins {
    id("xyz.deftu.gradle.multiversion-root")
}

preprocess {
    val fabric11904 = createNode("1.19.4-fabric", 1_19_04, "yarn")
    val forge11904 = createNode("1.19.4-forge", 1_19_04, "srg")
    val fabric11902 = createNode("1.19.2-fabric", 1_19_02, "yarn")
    val forge11902 = createNode("1.19.2-forge", 1_19_02, "srg")
    val fabric11802 = createNode("1.18.2-fabric", 1_18_02, "yarn")
    val forge11802 = createNode("1.18.2-forge", 1_18_02, "srg")
    val fabric11605 = createNode("1.16.5-fabric", 1_16_05, "yarn")
    val forge11605 = createNode("1.16.5-forge", 1_16_05, "srg")
    val forge11202 = createNode("1.12.2-forge", 1_12_02, "srg")
    val forge10809 = createNode("1.8.9-forge", 1_08_09, "srg")

    fabric11904.link(forge11904)
    forge11904.link(forge11902)
    forge11902.link(fabric11902)
    fabric11902.link(fabric11802)
    fabric11802.link(forge11802)
    forge11802.link(forge11605)
    forge11605.link(fabric11605)
    forge11605.link(forge11202)
    forge11202.link(forge10809)
}
