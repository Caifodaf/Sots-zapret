package domain


interface WinwsLauncherService {
    /**
     * Подготавливает аргументы для запуска winws.exe из профиля
     * @param selectedProfileFileName имя файла профиля
     * @param adaptProfile если true, адаптирует профиль через ProfileAdapterService
     * @return подготовленные аргументы для запуска winws.exe
     */
    suspend fun prepareProfileArgs(selectedProfileFileName: String, adaptProfile: Boolean = true): String
    
    /**
     * Получает путь к winws.exe
     * @return путь к winws.exe
     */
    fun getWinwsPath(): String
    
    /**
     * Валидирует аргументы на наличие запрещенных символов
     * @param args аргументы для проверки
     * @return true если аргументы валидны
     */
    fun validateArgs(args: String): Boolean
}
