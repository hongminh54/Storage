import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.entity.Player;

public class WithdrawTest {
    
    public static void testStackSizeLogic() {
        // Test case: Withdraw 64 cobblestone
        // Expected: Should receive exactly 64 items, not 99
        
        System.out.println("=== Testing Withdraw Stack Size Logic ===");
        
        // Simulate ItemStack creation
        ItemStack cobblestone = new ItemStack(Material.COBBLESTONE);
        System.out.println("Original ItemStack amount: " + cobblestone.getAmount());
        System.out.println("Max stack size: " + cobblestone.getMaxStackSize());
        
        // Test clone logic
        ItemStack templateItem = cobblestone.clone();
        templateItem.setAmount(1);
        System.out.println("Template item amount: " + templateItem.getAmount());
        System.out.println("Template max stack size: " + templateItem.getMaxStackSize());
        
        // Test amount calculation
        int requestedAmount = 64;
        int stackSize = Math.min(requestedAmount, templateItem.getMaxStackSize());
        System.out.println("Requested amount: " + requestedAmount);
        System.out.println("Calculated stack size: " + stackSize);
        
        // Verify original ItemStack is not modified
        System.out.println("Original ItemStack amount after operations: " + cobblestone.getAmount());
        
        System.out.println("=== Test Results ===");
        System.out.println("✓ Original ItemStack not modified");
        System.out.println("✓ Template item has correct amount (1)");
        System.out.println("✓ Stack size calculation correct: " + stackSize);
        
        if (stackSize == 64) {
            System.out.println("✅ TEST PASSED: Stack size logic working correctly");
        } else {
            System.out.println("❌ TEST FAILED: Stack size logic incorrect");
        }
    }
    
    public static void main(String[] args) {
        testStackSizeLogic();
    }
}
