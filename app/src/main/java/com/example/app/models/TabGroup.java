package com.example.app.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a group of browser tabs
 */
public class TabGroup {
    private long id;
    private String name;
    private String color;
    private List<BrowserTab> tabs;
    private long createdAt;
    private long updatedAt;
    
    public TabGroup() {
        this.tabs = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }
    
    public TabGroup(String name, String color) {
        this();
        this.name = name;
        this.color = color;
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public List<BrowserTab> getTabs() {
        return tabs;
    }
    
    public void setTabs(List<BrowserTab> tabs) {
        this.tabs = tabs;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void addTab(BrowserTab tab) {
        this.tabs.add(tab);
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void removeTab(BrowserTab tab) {
        this.tabs.remove(tab);
        this.updatedAt = System.currentTimeMillis();
    }
    
    public boolean containsTab(BrowserTab tab) {
        return this.tabs.contains(tab);
    }
    
    public int getTabCount() {
        return this.tabs.size();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TabGroup tabGroup = (TabGroup) obj;
        return id == tabGroup.id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return "TabGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", tabCount=" + tabs.size() +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
