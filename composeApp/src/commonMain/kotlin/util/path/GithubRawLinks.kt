package util.path

object GithubRawLinks {

    // Raw hosts файл (Telegram/Discord и др.)
    const val HOSTS_URL =
        "https://raw.githubusercontent.com/Flowseal/zapret-discord-youtube/refs/heads/main/.service/hosts"

    // GitHub API: содержимое папки lists (динамический список файлов)
    const val LISTS_CONTENT_URL =
        "https://api.github.com/repos/Caifodaf/Sots-zapret/contents/base/lists"

    // GitHub API: содержимое папки base/win
    const val WIN_FOLDER_CONTENT_URL =
        "https://api.github.com/repos/Caifodaf/Sots-zapret/contents/base/win?ref=master"

    // Базовый URL для contents API
    const val SOTS_ZAPRET_CONTENTS_BASE =
        "https://api.github.com/repos/Caifodaf/Sots-zapret/contents"

    // Профили в формате CSV (id, content, name, provider, service)
    const val PROFILES_CSV_URL =
        "https://raw.githubusercontent.com/Caifodaf/Sots-zapret/refs/heads/master/base/profiles_rows.csv"

    // Версии в формате CSV (id, type, version)
    const val VERSIONS_CSV_URL =
        "https://raw.githubusercontent.com/Caifodaf/Sots-zapret/refs/heads/master/base/version_rows.csv"
}
