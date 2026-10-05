import { Injectable } from '@angular/core';
import { ShopProfile } from './shop.service';

const DEFAULTS = { corMenu: '#52695F', corMenuAtivo: '#E3ECD9', corDestaque: '#4D7063' };

@Injectable({ providedIn: 'root' })
export class ShopThemeService {
  apply(profile: Partial<Pick<ShopProfile, 'corMenu' | 'corMenuAtivo' | 'corDestaque'>>) {
    const menu = this.color(profile.corMenu, DEFAULTS.corMenu);
    const active = this.color(profile.corMenuAtivo, DEFAULTS.corMenuAtivo);
    const accent = this.color(profile.corDestaque, DEFAULTS.corDestaque);
    const root = document.documentElement.style;
    root.setProperty('--office-sidebar', menu);
    root.setProperty('--office-sidebar-active', active);
    root.setProperty('--office-sidebar-active-ink', this.contrast(active));
    root.setProperty('--brand', accent);
    root.setProperty('--brand-strong', this.adjust(accent, -24));
    root.setProperty('--brand-soft', this.tint(accent, 88));
  }
  private color(value: string | undefined, fallback: string) {
    return /^#[0-9a-f]{6}$/i.test(value ?? '') ? value!.toUpperCase() : fallback;
  }
  private contrast(color: string) {
    const [r, g, b] = this.rgb(color);
    return (r * 299 + g * 587 + b * 114) / 1000 > 150 ? '#263D34' : '#FFFFFF';
  }
  private tint(color: string, amount: number) {
    return '#' + this.rgb(color).map(value => Math.round(value + (255 - value) * amount / 100).toString(16).padStart(2, '0')).join('').toUpperCase();
  }
  private adjust(color: string, amount: number) {
    return '#' + this.rgb(color).map(value => Math.max(0, Math.min(255, value + amount)).toString(16).padStart(2, '0')).join('').toUpperCase();
  }
  private rgb(color: string) { return [1, 3, 5].map(index => Number.parseInt(color.slice(index, index + 2), 16)); }
}
