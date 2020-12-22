object Ci {

    // this is the version used for building snapshots
    // .GITHUB_RUN_NUMBER-snapshot will be appended
    private const val snapshotBase = "0.0.2"

    private val githubRunNumber = System.getenv("GITHUB_RUN_NUMBER")

    private val snapshotVersion = when (githubRunNumber) {
        null -> "$snapshotBase-SNAPSHOT"
        else -> "$snapshotBase.$githubRunNumber-SNAPSHOT"
    }

    private val releaseVersion = System.getenv("RELEASE_VERSION")

    val isRelease = releaseVersion != null
    val version = releaseVersion ?: snapshotVersion
}
