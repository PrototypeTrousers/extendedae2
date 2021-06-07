package trousers.extendedae2.client.gui;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ScreenRegistration;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;
import appeng.container.AEBaseContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import trousers.extendedae2.client.gui.implementations.ExtendedInterfaceScreen;
import trousers.extendedae2.client.gui.implementations.ExtendedInterfaceTerminalScreen;
import trousers.extendedae2.container.implementations.ExtendedInterfaceContainer;
import trousers.extendedae2.container.implementations.ExtendedInterfaceTerminalContainer;

import java.io.FileNotFoundException;

public class EAEScreenRegistration extends ScreenRegistration {
    
    public static void register() {
        register(ExtendedInterfaceContainer.TYPE, ExtendedInterfaceScreen::new, "/screens/extended_interface.json");
        register(ExtendedInterfaceTerminalContainer.TYPE, ExtendedInterfaceTerminalScreen::new, "/screens/extended_terminal_interface.json");
    }
    
    public static <M extends AEBaseContainer, U extends AEBaseScreen<M>> void register(ContainerType<M> type,
                                                                                       StyledScreenFactory<M, U> factory,
                                                                                       String stylePath) {
        ScreenManager.<M, U>registerFactory(type, (container, playerInv, title) -> {
            ScreenStyle style;
            try {
                style = StyleManager.loadStyleDoc(stylePath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Failed to read Screen JSON file: " + stylePath + ": " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read Screen JSON file: " + stylePath, e);
            }
        
            return factory.create(container, playerInv, title, style);
        });
    }
    
}