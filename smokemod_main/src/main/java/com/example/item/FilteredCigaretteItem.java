package com.example.item;

/**
 * Сигарета с фильтром — никотин работает как обычно,
 * но эффект серости (desaturation) полностью отсутствует.
 */
public class FilteredCigaretteItem extends CigaretteItem {

    public FilteredCigaretteItem(Settings settings) {
        super(settings);
    }

    @Override
    protected float getDesatPuffAmount() { return 0.0f; }

    @Override
    protected float getDesatMax() { return 0.0f; }
}
