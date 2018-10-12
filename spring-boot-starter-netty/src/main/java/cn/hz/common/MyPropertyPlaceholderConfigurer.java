package cn.hz.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.util.List;


public class MyPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<String> propNamesList;

    @Override
    protected String convertProperty(String propertyName, String propertyValue) {
        if (isInPropNamesList(propertyName)) {
            //TODO: specific process, maybe limited log print
            log.info("key: " + propertyName + ", value: " + propertyValue);
            return "specific value";
        } else {
            log.info("key: " + propertyName + ", value: " + propertyValue);
            return propertyValue;
        }
    }

    /**
     * 判断是否是加密的属性
     * 
     * @param propertyName
     * @return
     */
    private boolean isInPropNamesList(String propertyName) {
        if (propNamesList.contains(propertyName)) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {

    }

    public List<String> getPropNamesList() {
        return propNamesList;
    }

    public void setPropNamesList(List<String> propNamesList) {
        this.propNamesList = propNamesList;
    }
}
