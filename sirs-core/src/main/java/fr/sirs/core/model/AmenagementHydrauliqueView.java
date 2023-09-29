/**
 *
 * This file is part of SIRS-Digues 2.
 *
 * Copyright (C) 2016, FRANCE-DIGUES,
 *
 * SIRS-Digues 2 is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * SIRS-Digues 2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SIRS-Digues 2. If not, see <http://www.gnu.org/licenses/>
 */

package fr.sirs.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author maximegavens
 */
public class AmenagementHydrauliqueView {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("superficie")
    private String superficie;

    @JsonProperty("capaciteStockage")
    private String capaciteStockage;

    @JsonProperty("profondeurMoyenne")
    private String profondeurMoyenne;

    @JsonProperty("designation")
    private String designation;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getSuperficie() {
        return superficie;
    }

    public String getCapaciteStockage() {
        return capaciteStockage;
    }

    public String getProfondeurMoyenne() {
        return profondeurMoyenne;
    }

    public String getDesignation() {
        return designation;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSuperficie(String superficie) {
        this.superficie = superficie;
    }

    public void setCapaciteStockage(String capaciteStockage) {
        this.capaciteStockage = capaciteStockage;
    }

    public void setProfondeurMoyenne(String profondeurMoyenne) {
        this.profondeurMoyenne = profondeurMoyenne;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}
