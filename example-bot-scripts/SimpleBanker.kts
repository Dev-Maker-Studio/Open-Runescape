import com.darkan.bot.scripts.ScriptCategory

@ScriptDescription(
    name = "Simple Banker",
    version = "1.0",
    author = "Claude",
    description = "Opens the nearest bank and deposits all items. Great for testing the banking API!",
    visible = true,
    category = ScriptCategory.OTHER
)
class SimpleBanker : BotScript() {

    private var itemsDeposited = 0
    private var banksOpened = 0

    override fun onStart() {
        println("=== Simple Banker Started ===")
        println("This script will repeatedly open banks and deposit items.")
        println("Position: ${getMyPlayerPosition()}")
        itemsDeposited = 0
        banksOpened = 0
    }

    override suspend fun loop() {
        // Check if we're already at a bank interface
        if (interfaceOpen(762)) {
            println("Bank is open! Depositing all items...")

            // Count items before depositing
            val itemCount = 28 - inventory.freeSlots()

            // Deposit everything
            bankAll()

            // Update stats
            itemsDeposited += itemCount

            println("Deposited $itemCount items (Total: $itemsDeposited)")

            // Close the bank
            delay(600, 200) // Random delay
            closeInterfaces()

            // Wait a bit before next cycle
            delay(2000, 500)
        } else {
            // Try to open a bank
            println("Opening nearest bank...")
            if (openClosestBank()) {
                banksOpened++
                println("Bank opened! (Total banks opened: $banksOpened)")

                // Wait for bank interface to open
                delayUntil(5000) { interfaceOpen(762) }
            } else {
                println("No bank found nearby!")
                delay(3000) // Wait before trying again
            }
        }
    }

    override fun onStop() {
        println("=== Simple Banker Stopped ===")
        println("Statistics:")
        println("  Banks Opened: $banksOpened")
        println("  Items Deposited: $itemsDeposited")
    }
}

// Return the script instance
SimpleBanker()
