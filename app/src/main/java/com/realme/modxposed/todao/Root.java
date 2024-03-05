package com.realme.modxposed.todao;

import java.util.ArrayList;
import java.util.List;

public class Root {
    ArrayList<Hooks> hooks;
    Search search;

    public ArrayList<Hooks> getHooks() {
        return hooks;
    }

    public void setHooks(ArrayList<Hooks> hooks) {
        this.hooks = hooks;
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }
}
