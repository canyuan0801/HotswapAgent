
package org.hotswap.agent.plugin.spring.files;

import org.hotswap.agent.plugin.spring.listener.SpringEvent;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.List;


public class PropertiesChangeEvent extends SpringEvent<List<PropertiesChangeEvent.PropertyChangeItem>> {
    
    public PropertiesChangeEvent(List<PropertyChangeItem> source, ConfigurableListableBeanFactory beanFactory) {
        super(source, beanFactory);
    }

    public static class PropertyChangeItem {
        public PropertyChangeItem(String key, String oldValue, String newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        private String key;
        private String oldValue;
        private String newValue;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }
    }
}
