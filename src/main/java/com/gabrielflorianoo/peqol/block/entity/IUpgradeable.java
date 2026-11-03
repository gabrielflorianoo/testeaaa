package com.gabrielflorianoo.peqol.block.entity;

/**
 * Machines that accept Alchemical Machine Upgrades (tier 0..{@link #MAX_TIER}).
 */
public interface IUpgradeable {

    int MAX_TIER = 3;

    int getTier();

    void setTier(int tier);
}
