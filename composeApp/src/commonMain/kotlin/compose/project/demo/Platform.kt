package compose.project.demo
import co.touchlab.kermit.Logger

const val AppTag = "InAPPPurchase"
val log: Logger = Logger.withTag(AppTag)

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform