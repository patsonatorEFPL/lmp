import { Component, AfterViewInit, OnDestroy, OnInit, signal, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SeoService } from '../../core/services/seo.service';

declare const L: any;

@Component({
  selector: 'lmp-map',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="relative">
      <div class="mx-auto max-w-7xl px-4 pt-12 pb-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
          Carte Interactive
        </h1>
        <p class="mt-4 text-lg text-(--muted-foreground)">
          Découvrez les solutions locales sur notre carte interactive
        </p>
      </div>

      <!-- Map container -->
      <div class="relative mt-6 h-[60vh] sm:h-[70vh]">
        <div id="map" class="h-full w-full"></div>

        <!-- Controls overlay -->
        <div class="absolute top-4 right-4 z-[1000] flex flex-col gap-2 w-64">
          <input
            type="text"
            placeholder="Rechercher une localisation..."
            [(ngModel)]="searchQuery"
            class="w-full rounded-sm border border-(--border) bg-(--card)/95 px-4 py-2.5 text-sm text-(--foreground) placeholder:text-(--muted-foreground) backdrop-blur-sm outline-none focus:border-(--primary)/50"
          />
          <select
            [(ngModel)]="selectedCategory"
            class="w-full rounded-sm border border-(--border) bg-(--card)/95 px-4 py-2.5 text-sm text-(--foreground) backdrop-blur-sm outline-none cursor-pointer"
          >
            <option value="">Toutes les catégories</option>
            <option value="restaurant">Restaurants</option>
            <option value="commerce">Commerces</option>
            <option value="service">Services</option>
            <option value="sante">Santé</option>
          </select>
          <p class="rounded-sm bg-(--card)/95 px-3 py-2 text-xs text-(--muted-foreground) backdrop-blur-sm border border-(--border)">
            ℹ️ Cliquez sur un marqueur pour plus d'informations
          </p>
        </div>
      </div>
    </section>
  `,
})
export class MapComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly seo = inject(SeoService);
  private readonly platformId = inject(PLATFORM_ID);

  searchQuery = signal('');
  selectedCategory = signal('');
  private map: any;

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'Carte Interactive — Solutions Locales',
      description: 'Explorez notre carte interactive pour découvrir les solutions locales et services de marketing digital près de chez vous. LMP Digital Services à Uccle, Bruxelles.',
      url: '/map',
      keywords: 'carte interactive, solutions locales, marketing digital Bruxelles, services locaux Uccle',
    });
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.initMap();
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  private initMap(): void {
    // Load Leaflet CSS dynamically
    if (!document.querySelector('link[href*="leaflet"]')) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(link);
    }

    // Load Leaflet JS dynamically
    if (typeof L === 'undefined') {
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => this.createMap();
      document.head.appendChild(script);
    } else {
      this.createMap();
    }
  }

  private createMap(): void {
    // Center on Uccle, Belgium (LMP office location)
    this.map = L.map('map').setView([50.8012, 4.3388], 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);

    // Add LMP office marker
    L.marker([50.8012, 4.3388])
      .addTo(this.map)
      .bindPopup(`
        <div style="font-family: sans-serif; min-width: 200px;">
          <strong style="font-size: 14px;">LMP Digital Services</strong><br/>
          <span style="color: #666; font-size: 12px;">Rue Gatti De Gamond 97<br/>1180 Uccle, Belgique</span><br/>
          <a href="mailto:lmp.assistance@gmail.com" style="color: #10b981; font-size: 12px;">lmp.assistance@gmail.com</a>
        </div>
      `);
  }
}
