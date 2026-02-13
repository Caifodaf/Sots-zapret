package domain.repository

interface WhitelistManager {
    fun deleteLink(link: String): Result<Unit>
    fun saveWhitelist(links: List<String>): Result<Unit>
    fun addNewLink(newLink: String): Result<Unit>
    fun getWhiteList(): List<String>
    fun mergeWhiteList(local: List<String>, api: List<String>): List<String>
} 