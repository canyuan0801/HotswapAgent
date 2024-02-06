
package org.hotswap.agent.plugin.hibernate_jakarta.testEntitiesHotswap;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;


@Entity
public class TestEntity2 {
    @Id
    @GeneratedValue
    private Long id;

    private String name;


    @Transient
    private String description;

    public TestEntity2() {
    }

    public TestEntity2(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
