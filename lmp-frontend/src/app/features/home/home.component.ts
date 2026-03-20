import {
  Component,
  signal,
  AfterViewInit,
  OnDestroy,
  OnInit,
  Inject,
  PLATFORM_ID,
  inject,
} from '@angular/core';
import { isPlatformBrowser, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SeoService } from '../../core/services/seo.service';
import {
  LucideAngularModule,
  ArrowRight,
  Check,
<<<<<<< HEAD
  Calendar,
  ShoppingCart,
  Search,
  BarChart3,
  Globe,
  Megaphone,
  Target,
  Layers,
  Rocket,
  ChevronDown,
  Quote,
  Shield,
  Users,
  Award,
  Zap,
  TrendingUp,
  Star,
=======
  ChevronDown,
  Calendar,
  ShoppingCart,
  Send,
  ExternalLink,
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
} from 'lucide-angular';
import { AppointmentModalComponent } from '../../shared/modals/appointment-modal.component';
import { CatalogService, ServiceItem } from '../../core/services/catalog.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'lmp-home',
  standalone: true,
  imports: [RouterLink, LucideAngularModule, AppointmentModalComponent, CurrencyPipe, FormsModule],
  template: `
    <!-- ===== HERO SECTION ===== -->
<<<<<<< HEAD
    <section class="relative overflow-hidden min-h-screen -mt-16 pt-16 flex items-center">
=======
    <section
      class="relative overflow-hidden min-h-screen -mt-16 pt-16 flex items-center"
    >
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
      <!-- Background image with overlay -->
      <div class="absolute inset-0">
        <img
          src="/images/hero-bg-office.jpg"
<<<<<<< HEAD
          alt="Modern corporate office — Photo by Mikael Blomkvist"
          class="absolute inset-0 w-full h-full object-cover"
        />
        <div class="absolute inset-0 bg-gradient-to-b from-black/80 via-black/70 to-black/90"></div>
=======
          alt="Modern corporate office"
          class="absolute inset-0 w-full h-full object-cover"
        />
        <div class="absolute inset-0 bg-gradient-to-br from-[#040806]/95 via-[#050d0a]/90 to-[#030a07]/85"></div>
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
      </div>

      <!-- Subtle dot pattern -->
      <div
<<<<<<< HEAD
        class="pointer-events-none absolute inset-0 opacity-20"
        style="background-image: radial-gradient(circle, rgba(16,185,129,0.3) 1px, transparent 1px); background-size: 40px 40px;"
      ></div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8 w-full">
        <div class="max-w-3xl">
          <!-- Service pills -->
          <div class="scroll-animate anim-fade-up mb-8 flex flex-wrap gap-2">
            @for (tag of heroTags; track tag) {
              <span class="rounded-full border border-white/15 bg-white/5 backdrop-blur-sm px-4 py-1.5 text-xs font-medium tracking-wider text-white/70 uppercase">
                {{ tag }}
              </span>
            }
          </div>

          <!-- Main heading -->
          <h1 class="scroll-animate anim-fade-up delay-100 font-display tracking-tight text-white leading-[1.1]">
            <span class="block text-5xl sm:text-6xl lg:text-7xl font-light italic text-emerald-400">
              Propulsez
            </span>
            <span class="block text-5xl sm:text-6xl lg:text-7xl font-bold mt-1">
              Votre Visibilité
            </span>
            <span class="block text-5xl sm:text-6xl lg:text-7xl font-bold">
              Digitale<span class="text-emerald-400">.</span>
            </span>
          </h1>

          <p class="scroll-animate anim-fade-up delay-200 mt-6 max-w-xl text-base leading-relaxed text-white/60 sm:text-lg font-light">
            Technologie de pointe et expertise humaine pour dominer
            votre marché local. Plus de 500 entreprises nous font confiance.
          </p>

          <!-- CTA buttons -->
          <div class="scroll-animate anim-fade-up delay-300 mt-10 flex flex-wrap gap-4">
            <button
              (click)="showAppointment.set(true)"
              class="group inline-flex items-center gap-2.5 rounded-full bg-emerald-500 px-7 py-3.5 text-sm font-semibold text-white transition-all duration-300 hover:bg-emerald-400 hover:shadow-lg hover:shadow-emerald-500/25 cursor-pointer"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver un audit gratuit
              <lucide-icon [img]="ArrowRightIcon" [size]="16" class="transition-transform group-hover:translate-x-0.5"></lucide-icon>
            </button>
            <a
              routerLink="/services"
              class="inline-flex items-center gap-2 rounded-full border border-white/20 px-7 py-3.5 text-sm font-medium text-white/90 transition-all duration-300 hover:bg-white/10 hover:border-white/30 cursor-pointer"
            >
              Découvrir nos services
            </a>
          </div>

          <!-- Trust strip -->
          <div class="scroll-animate anim-fade-up delay-400 mt-12 flex flex-wrap items-center gap-6 text-xs text-white/40 font-medium uppercase tracking-wider">
            <span class="flex items-center gap-2">
              <lucide-icon [img]="ShieldIcon" [size]="14" class="text-emerald-500"></lucide-icon>
              Paiement sécurisé
            </span>
            <span class="flex items-center gap-2">
              <lucide-icon [img]="ZapIcon" [size]="14" class="text-emerald-500"></lucide-icon>
              Mise en place rapide
            </span>
            <span class="flex items-center gap-2">
              <lucide-icon [img]="CheckIcon" [size]="14" class="text-emerald-500"></lucide-icon>
              Pas de frais cachés
            </span>
=======
        class="pointer-events-none absolute inset-0"
        style="background-image: radial-gradient(circle, rgba(16,185,129,0.12) 1px, transparent 1px); background-size: 40px 40px; mask-image: radial-gradient(80% 80%, black 40%, transparent 100%);"
      ></div>

      <!-- Particles -->
      @for (p of particles; track $index) {
        <div
          class="hero-particle"
          [style.left.%]="p.x"
          [style.top.%]="p.y"
          [style.width.px]="p.size"
          [style.height.px]="p.size"
          [style.animation-duration]="p.duration + 's'"
          [style.animation-delay]="p.delay + 's'"
        ></div>
      }

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8 w-full">
        <div class="grid grid-cols-1 items-center gap-16 lg:grid-cols-2">
          <!-- Left column: Large editorial headline -->
          <div class="scroll-animate anim-fade-up">
            <h1 class="text-5xl sm:text-6xl lg:text-7xl xl:text-8xl font-bold tracking-tight text-white leading-[0.95]">
              Propulsez,
              <br />
              <span class="font-editorial-italic text-emerald-accent">Votre Visibilité</span>
            </h1>
            <p class="mt-6 text-3xl sm:text-4xl font-light text-white/60 tracking-tight">
              L'excellence digitale.
            </p>
          </div>

          <!-- Right column: Description + CTA + Social -->
          <div class="scroll-animate anim-fade-up delay-200">
            <div class="border-t border-white/10 pt-8">
              <p class="text-base sm:text-lg text-white/70 leading-relaxed max-w-lg">
                <span class="text-white font-medium">© Nous bâtissons des stratégies digitales,</span>
                sites web et expériences numériques avec précision, clarté et engagement.
              </p>
            </div>

            <div class="mt-8 border-t border-white/10 pt-8 flex items-center justify-between">
              <button
                (click)="showAppointment.set(true)"
                class="inline-flex items-center gap-2 rounded-full bg-white px-6 py-3 text-sm font-semibold text-gray-900 transition-all hover:bg-emerald-accent hover:text-white cursor-pointer"
              >
                Réserver un audit gratuit
              </button>

              <!-- Social icons -->
              <div class="hidden sm:flex items-center gap-3">
                <a href="#" aria-label="Facebook" class="flex h-10 w-10 items-center justify-center rounded-full border border-white/15 text-white/50 transition-colors hover:border-emerald-accent/50 hover:text-emerald-accent">
                  <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
                </a>
                <a href="#" aria-label="Instagram" class="flex h-10 w-10 items-center justify-center rounded-full border border-white/15 text-white/50 transition-colors hover:border-emerald-accent/50 hover:text-emerald-accent">
                  <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24"><path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 1 0 0 12.324 6.162 6.162 0 0 0 0-12.324zM12 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm6.406-11.845a1.44 1.44 0 1 0 0 2.881 1.44 1.44 0 0 0 0-2.881z"/></svg>
                </a>
                <a href="#" aria-label="LinkedIn" class="flex h-10 w-10 items-center justify-center rounded-full border border-white/15 text-white/50 transition-colors hover:border-emerald-accent/50 hover:text-emerald-accent">
                  <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg>
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Bottom: Service pills -->
        <div class="mt-16 flex flex-wrap items-center gap-8 scroll-animate anim-fade-up delay-300">
          <span class="text-sm font-medium text-white/40">Nos Expertises</span>
          <div class="flex flex-wrap items-center gap-3">
            @for (tag of serviceTags; track tag) {
              <span class="rounded-full border border-white/10 bg-white/5 px-4 py-1.5 text-sm text-white/70 transition-colors hover:border-emerald-accent/30 hover:text-emerald-accent">
                {{ tag }}
              </span>
            }
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
          </div>
        </div>
      </div>
    </section>

    <!-- ===== PARTNER TICKER ===== -->
    <section class="border-y border-(--border) bg-(--card)/50 overflow-hidden">
<<<<<<< HEAD
      <div class="py-5">
        <div class="marquee-track">
          @for (partner of partnerLogos.concat(partnerLogos); track $index) {
            <span class="flex items-center gap-2 px-8 text-sm font-medium tracking-wider uppercase whitespace-nowrap text-(--muted-foreground)/60">
              <span class="h-1.5 w-1.5 rounded-full bg-(--primary)"></span>
              {{ partner }}
            </span>
=======
      <div class="py-6">
        <div class="ticker-track">
          @for (i of [0,1]; track i) {
            <div class="flex items-center gap-12 px-6">
              @for (partner of partners; track partner.name) {
                <span class="flex items-center gap-2 text-sm font-medium text-(--muted-foreground) whitespace-nowrap opacity-60 hover:opacity-100 transition-opacity">
                  <span class="text-lg">{{ partner.icon }}</span>
                  {{ partner.name }}
                </span>
              }
            </div>
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
          }
        </div>
      </div>
    </section>

<<<<<<< HEAD
    <!-- ===== SERVICES SECTION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <!-- Section header -->
        <div class="flex flex-col lg:flex-row lg:items-end lg:justify-between mb-16">
          <div class="scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
              Ce Que Nous Faisons
            </span>
            <h2 class="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-(--foreground) leading-tight">
              Des solutions qui transforment<br />
              <span class="italic font-light text-(--primary)">votre présence digitale</span>
            </h2>
          </div>
          <div class="mt-6 lg:mt-0 scroll-animate anim-fade-right delay-200">
            <a
              routerLink="/services"
              class="inline-flex items-center gap-2 text-sm font-medium text-(--primary) hover:underline underline-offset-4 cursor-pointer"
            >
              Voir tous les services
              <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
            </a>
          </div>
        </div>

        <!-- Skeleton loading -->
        @if (loadingFeatured()) {
          <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
            @for (skeleton of [1,2,3,4]; track skeleton) {
              <div class="flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 animate-pulse">
                <div class="h-4 w-12 rounded bg-(--muted) mb-4"></div>
                <div class="h-6 w-3/4 rounded bg-(--muted) mb-3"></div>
                <div class="h-4 w-full rounded bg-(--muted) mb-2"></div>
                <div class="h-4 w-5/6 rounded bg-(--muted)"></div>
=======
    <!-- ===== SERVICES: WHAT WE DO ===== -->
    <section id="service" class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <!-- Split header -->
        <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between mb-16">
          <div class="scroll-animate anim-fade-up">
            <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-4">
              Nos Services
            </div>
            <h2 class="text-4xl sm:text-5xl font-bold text-(--foreground) leading-tight">
              <span class="font-editorial-italic text-(--primary)">Ce Que</span>
              Nous Faisons
            </h2>
          </div>
          <p class="max-w-lg text-(--muted-foreground) scroll-animate anim-fade-up delay-100">
            Des solutions digitales de l'idée au lancement — alliant stratégie,
            design et technologie pour bâtir des résultats qui performent.
          </p>
        </div>

        <!-- Service cards 2x2 grid -->
        <div class="grid grid-cols-1 gap-px rounded-2xl border border-(--border) overflow-hidden sm:grid-cols-2">
          @for (svc of coreServices; track svc.title; let i = $index) {
            <div
              class="group relative bg-(--card) p-8 sm:p-10 transition-all duration-300 hover:bg-(--primary)/5 scroll-animate anim-fade-up"
              [class]="'group relative bg-(--card) p-8 sm:p-10 transition-all duration-300 hover:bg-(--primary)/5 scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
            >
              <div class="flex items-center justify-center h-16 w-16 rounded-2xl bg-(--muted) mb-6 text-2xl transition-transform duration-300 group-hover:scale-110">
                {{ svc.icon }}
              </div>
              <span class="text-xs font-mono tracking-wider text-(--muted-foreground)">/{{ (i + 1).toString().padStart(2, '0') }}</span>
              <h3 class="mt-2 text-lg font-bold text-(--foreground)">{{ svc.title }}</h3>
              <p class="mt-3 text-sm leading-relaxed text-(--muted-foreground)">{{ svc.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== ABOUT + STATS ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-6">
            À Propos
          </div>
          <p class="max-w-4xl text-2xl sm:text-3xl lg:text-4xl font-light leading-snug text-(--foreground)">
            Nous aidons les
            <span class="font-editorial-italic text-(--primary)">entreprises ambitieuses</span>
            à bâtir une présence digitale qui se démarque et qui convertit. Nous croyons en l'efficacité, la rapidité d'exécution et le design orienté résultats.
          </p>
        </div>

        <!-- Stats row -->
        <div class="mt-16 grid grid-cols-1 gap-8 sm:grid-cols-3">
          @for (stat of stats; track stat.label; let i = $index) {
            <div
              class="scroll-animate anim-fade-up"
              [class]="'scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
            >
              <div class="text-5xl sm:text-6xl font-bold text-(--foreground) font-editorial">{{ stat.value }}</div>
              <div class="mt-1 text-sm text-(--muted-foreground)">{{ stat.sublabel }}</div>
              <div class="mt-2 text-base font-semibold text-(--foreground)">{{ stat.label }}</div>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== FEATURED SERVICES SHOWCASE ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="text-center mb-14 scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-4">
            Catalogue
          </div>
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Solutions
            <span class="font-editorial-italic text-(--primary)">Premium</span>
          </h2>
          <p class="mt-4 mx-auto max-w-2xl text-(--muted-foreground)">
            Une gamme complète d'outils et de services pour dominer votre marché local et national.
          </p>
        </div>

        <!-- Loading skeleton -->
        @if (loadingFeatured()) {
          <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            @for (s of [1,2,3,4,5,6]; track s) {
              <div class="rounded-2xl border border-(--border) bg-(--card) p-6 animate-pulse">
                <div class="flex items-start justify-between mb-4">
                  <div class="h-12 w-12 rounded-xl bg-(--muted)"></div>
                  <div class="h-5 w-20 rounded-full bg-(--muted)"></div>
                </div>
                <div class="h-5 w-3/4 rounded bg-(--muted)"></div>
                <div class="mt-3 space-y-2">
                  <div class="h-3 w-full rounded bg-(--muted)"></div>
                  <div class="h-3 w-5/6 rounded bg-(--muted)"></div>
                </div>
                <div class="mt-5 flex items-end justify-between border-t border-(--border) pt-4">
                  <div class="h-6 w-24 rounded bg-(--muted)"></div>
                  <div class="h-8 w-8 rounded-lg bg-(--muted)"></div>
                </div>
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
              </div>
            }
          </div>
        }

        <!-- Service cards from API -->
        <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
          @for (service of featuredServices(); track service.id; let i = $index) {
            <a
              routerLink="/services"
              [fragment]="service.slug"
<<<<<<< HEAD
              class="group relative flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 cursor-pointer scroll-animate anim-fade-up"
              [class]="'group relative flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 cursor-pointer scroll-animate anim-fade-up delay-' + ((i % 4) + 1) + '00'"
            >
              <!-- Number -->
              <span class="font-mono text-xs font-semibold text-(--muted-foreground)/40 mb-4">
                /{{ (i + 1).toString().padStart(2, '0') }}
              </span>
              <div class="flex items-start justify-between gap-4 mb-3">
                <h3 class="font-display text-lg font-semibold text-(--foreground) group-hover:text-(--primary) transition-colors">
                  {{ service.title }}
                </h3>
                <span class="shrink-0 rounded-full bg-(--primary)/10 px-2.5 py-0.5 text-xs font-medium text-(--primary)">
                  {{ service.categoryName }}
                </span>
              </div>
              <p class="text-sm leading-relaxed text-(--muted-foreground) flex-1">
                {{ service.description }}
              </p>
=======
              class="group flex flex-col rounded-2xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-emerald-accent/30 hover:shadow-lg hover:shadow-emerald-accent/5 hover:-translate-y-1 scroll-animate anim-fade-up cursor-pointer"
              [class]="'group flex flex-col rounded-2xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-emerald-accent/30 hover:shadow-lg hover:shadow-emerald-accent/5 hover:-translate-y-1 scroll-animate anim-fade-up cursor-pointer delay-' + ((i % 3) + 1) + '00'"
            >
              <div class="flex items-start justify-between mb-4">
                <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-(--primary)/10 text-xl transition-transform duration-300 group-hover:scale-110">
                  {{ service.icon }}
                </div>
                <span class="rounded-full bg-(--primary)/10 px-2.5 py-0.5 text-xs font-medium text-(--primary)">
                  {{ service.categoryName }}
                </span>
              </div>
              <h3 class="text-base font-bold text-(--foreground)">{{ service.title }}</h3>
              <p class="mt-2 flex-1 text-sm leading-relaxed text-(--muted-foreground)">{{ service.description }}</p>
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
              <ul class="mt-4 space-y-1.5">
                @for (benefit of service.benefits?.slice(0, 3); track benefit) {
                  <li class="flex items-start gap-2 text-xs text-(--muted-foreground)">
                    <lucide-icon [img]="CheckIcon" [size]="12" class="mt-0.5 text-(--primary) shrink-0"></lucide-icon>
                    {{ benefit }}
                  </li>
                }
              </ul>
              <div class="mt-5 flex items-end justify-between border-t border-(--border) pt-4">
                <div>
                  @if (service.currentOffer) {
                    <span class="text-xl font-bold text-(--primary)">
                      {{ service.currentOffer.price | currency:'EUR':'symbol':'1.2-2':'fr' }}
                    </span>
                    <span class="text-xs text-(--muted-foreground) ml-1">
                      {{ service.currentOffer.durationType === 'ONE_TIME' ? 'unique' :
                         service.currentOffer.durationType === 'MONTHLY' ? '/ mois' :
                         service.currentOffer.durationType === 'YEARLY' ? '/ an' : '' }}
                    </span>
                  } @else {
                    <span class="text-lg font-bold text-(--primary)">Sur devis</span>
                  }
                </div>
                <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary) transition-colors group-hover:bg-(--primary)/20">
                  <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
                </div>
              </div>
            </a>
          }
        </div>

<<<<<<< HEAD
        <!-- Fallback static services -->
=======
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
        @if (featuredServices().length === 0 && !loadingFeatured()) {
          <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
            @for (service of fallbackServices; track service.subtitle; let i = $index) {
              <a
                routerLink="/services"
<<<<<<< HEAD
                class="group relative flex flex-col rounded-xl border border-(--border) bg-(--card) p-7 transition-all duration-300 hover:border-(--primary)/30 hover:shadow-lg hover:shadow-emerald-500/5 cursor-pointer scroll-animate anim-fade-up"
              >
                <span class="font-mono text-xs font-semibold text-(--muted-foreground)/40 mb-4">
                  /{{ (i + 1).toString().padStart(2, '0') }}
                </span>
                <h3 class="font-display text-lg font-semibold text-(--foreground) group-hover:text-(--primary) transition-colors mb-2">
                  {{ service.subtitle }}
                </h3>
                <p class="text-sm leading-relaxed text-(--muted-foreground) flex-1">
                  {{ service.description }}
                </p>
=======
                class="group flex flex-col rounded-2xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-emerald-accent/30 hover:shadow-lg hover:shadow-emerald-accent/5 hover:-translate-y-1 scroll-animate anim-fade-up cursor-pointer"
              >
                <div class="flex items-start justify-between mb-4">
                  <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-(--primary)/10 text-xl">
                    {{ service.emoji }}
                  </div>
                  <span class="rounded-full bg-(--primary)/10 px-2.5 py-0.5 text-xs font-medium text-(--primary)">
                    {{ service.category }}
                  </span>
                </div>
                <h3 class="text-base font-bold text-(--foreground)">{{ service.subtitle }}</h3>
                <p class="mt-2 flex-1 text-sm leading-relaxed text-(--muted-foreground)">{{ service.description }}</p>
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
                <div class="mt-5 flex items-end justify-between border-t border-(--border) pt-4">
                  <span class="text-xl font-bold text-(--primary)">{{ service.price }}</span>
                  <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary)">
                    <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
                  </div>
                </div>
              </a>
            }
          </div>
        }
      </div>
    </section>

<<<<<<< HEAD
    <!-- ===== ABOUT / STATS SECTION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
          <!-- Left: Statement -->
          <div class="scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-4 block">
              Notre Histoire
            </span>
            <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground) leading-snug">
              Nous aidons les marques ambitieuses à
              <span class="italic font-light text-(--primary)">dominer</span>
              leur marché local depuis plus de 10 ans.
            </h2>
            <p class="mt-6 text-(--muted-foreground) leading-relaxed">
              Notre approche combine analyse de données, expertise humaine et compréhension
              profonde des enjeux locaux. Chaque stratégie est taillée sur mesure pour maximiser
              votre retour sur investissement.
            </p>
          </div>

          <!-- Right: Stats -->
          <div class="grid grid-cols-3 gap-4 scroll-animate anim-scale-in delay-200">
            @for (stat of aboutStats; track stat.label) {
              <div class="text-center rounded-xl border border-(--border) bg-(--card) p-6">
                <div class="font-display text-3xl sm:text-4xl font-bold text-(--primary)">{{ stat.value }}</div>
                <div class="mt-2 text-xs font-medium uppercase tracking-wider text-(--muted-foreground)">{{ stat.label }}</div>
              </div>
=======
        <div class="mt-10 text-center scroll-animate anim-fade-up">
          <a
            routerLink="/services"
            class="inline-flex items-center gap-2 rounded-full border border-(--border) bg-(--card) px-6 py-3 text-sm font-medium text-(--foreground) transition-colors hover:bg-(--accent) cursor-pointer"
          >
            Voir tous les services
            @if (totalFeaturedCount() > 6) {
              <span class="text-xs text-(--muted-foreground)">(+{{ totalFeaturedCount() - 6 }} autres)</span>
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== PROCESS SECTION ===== -->
<<<<<<< HEAD
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="text-center mb-16 scroll-animate anim-fade-up">
          <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
            Notre Processus
          </span>
          <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground)">
            De l'audit à la croissance,
            <span class="italic font-light text-(--primary)">en 4 étapes</span>
          </h2>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          @for (step of processSteps; track step.title; let i = $index) {
            <div
              class="scroll-animate anim-fade-up relative rounded-xl border border-(--border) bg-(--card) p-6"
              [class]="'scroll-animate anim-fade-up relative rounded-xl border border-(--border) bg-(--card) p-6 delay-' + (i + 1) + '00'"
            >
              <!-- Step number -->
              <div class="font-mono text-5xl font-bold text-(--primary)/10 absolute top-4 right-5">
                {{ (i + 1).toString().padStart(2, '0') }}
              </div>
              <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary) mb-4">
                <lucide-icon [img]="step.icon" [size]="20"></lucide-icon>
              </div>
              <h3 class="font-display text-base font-semibold text-(--foreground) mb-2">{{ step.title }}</h3>
              <p class="text-sm leading-relaxed text-(--muted-foreground)">{{ step.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== TESTIMONIALS SECTION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between mb-16">
          <div class="scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
              Ce Qu'ils Disent
            </span>
            <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground)">
              Ils ont transformé<br />
              <span class="italic font-light text-(--primary)">leur visibilité</span>
            </h2>
          </div>
          <div class="mt-4 sm:mt-0 flex items-center gap-2 scroll-animate anim-fade-right delay-200">
            <button
              class="flex h-10 w-10 items-center justify-center rounded-full border border-(--border) text-(--muted-foreground) transition-colors hover:text-(--foreground) hover:border-(--primary)/30 cursor-pointer"
              (click)="prevTestimonial()"
              aria-label="Témoignage précédent"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button
              class="flex h-10 w-10 items-center justify-center rounded-full bg-(--primary) text-white transition-colors hover:bg-emerald-600 cursor-pointer"
              (click)="nextTestimonial()"
              aria-label="Témoignage suivant"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>

        <div class="grid grid-cols-1 gap-6 sm:grid-cols-3">
          @for (t of visibleTestimonials(); track t.name; let i = $index) {
            <div
              class="rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-fade-up"
              [class]="'rounded-xl border border-(--border) bg-(--card) p-6 scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
            >
              <!-- Stars -->
              <div class="flex gap-0.5 text-amber-400 mb-4">
                @for (s of [1,2,3,4,5]; track s) {
                  <lucide-icon [img]="StarIcon" [size]="14" class="fill-current"></lucide-icon>
                }
              </div>
              <p class="text-sm leading-relaxed text-(--muted-foreground) italic">
                "{{ t.quote }}"
              </p>
              <div class="mt-5 flex items-center gap-3 border-t border-(--border) pt-4">
                <img
                  [src]="t.avatar"
                  [alt]="t.name"
                  class="h-10 w-10 rounded-full object-cover"
                />
                <div>
                  <div class="text-sm font-semibold text-(--foreground)">{{ t.name }}</div>
                  <div class="text-xs text-(--muted-foreground)">{{ t.role }}</div>
=======
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between mb-16">
          <div class="scroll-animate anim-fade-up">
            <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-4">
              Notre Processus
            </div>
            <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground) leading-tight">
              Le Parcours Vers
              <br />
              <span class="font-editorial-italic text-(--primary)">Un Succès Digital</span>
            </h2>
          </div>
          <p class="max-w-lg text-(--muted-foreground) scroll-animate anim-fade-up delay-100">
            Nous gardons les choses simples et collaboratives — pour passer du concept au lancement sans chaos.
          </p>
        </div>

        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          @for (step of processSteps; track step.title; let i = $index) {
            <div
              class="rounded-2xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-emerald-accent/30 scroll-animate anim-fade-up"
              [class]="'rounded-2xl border border-(--border) bg-(--card) p-6 transition-all duration-300 hover:border-emerald-accent/30 scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
            >
              <span class="text-xs font-mono tracking-wider text-(--muted-foreground)">/{{ (i + 1).toString().padStart(2, '0') }}</span>
              <h3 class="mt-3 text-lg font-bold text-(--foreground)">{{ step.title }}</h3>
              <p class="mt-3 text-sm leading-relaxed text-(--muted-foreground)">{{ step.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== CERTIFICATIONS ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="text-center mb-14 scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-4">
            🏆 Nos Certifications
          </div>
        </div>

        <div class="space-y-0 scroll-animate anim-fade-up delay-100">
          @for (cert of certifications; track cert.title) {
            <div class="flex flex-col sm:flex-row sm:items-center justify-between py-6 border-b border-(--border) last:border-b-0 gap-2 sm:gap-4">
              <span class="text-sm text-(--muted-foreground) sm:w-40 shrink-0">{{ cert.source }}</span>
              <h3 class="text-lg sm:text-xl font-bold text-(--foreground) flex-1 text-center">{{ cert.title }}</h3>
              <span class="text-sm text-(--muted-foreground) sm:w-56 text-right shrink-0">{{ cert.project }}</span>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== TESTIMONIALS ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="mb-14 scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-4">
            Témoignages
          </div>
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Ce Que Disent
            <span class="font-editorial-italic text-(--primary)">Nos Clients</span>
          </h2>
        </div>

        <!-- Main testimonial -->
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <!-- Large featured testimonial -->
          <div class="lg:col-span-2 rounded-2xl border border-(--border) bg-(--card) p-8 sm:p-10 scroll-animate anim-fade-up">
            <p class="text-lg sm:text-xl leading-relaxed text-(--foreground) font-light">
              "{{ testimonials[0].quote }}"
            </p>
            <div class="mt-8 flex items-center gap-4">
              <img
                [src]="'https://i.pravatar.cc/80?u=' + testimonials[0].name"
                [alt]="testimonials[0].name"
                class="h-12 w-12 rounded-full object-cover"
              />
              <div>
                <div class="font-semibold text-(--foreground)">{{ testimonials[0].name }}</div>
                <div class="text-sm text-(--muted-foreground)">{{ testimonials[0].role }}</div>
              </div>
            </div>
          </div>

          <!-- Smaller testimonials stacked -->
          <div class="flex flex-col gap-6">
            @for (t of testimonials.slice(1); track t.name; let i = $index) {
              <div
                class="rounded-2xl border border-(--border) bg-(--card) p-6 scroll-animate anim-fade-up"
                [class]="'rounded-2xl border border-(--border) bg-(--card) p-6 scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
              >
                <p class="text-sm leading-relaxed text-(--muted-foreground)">
                  "{{ t.quote }}"
                </p>
                <div class="mt-4 flex items-center gap-3">
                  <img
                    [src]="'https://i.pravatar.cc/60?u=' + t.name"
                    [alt]="t.name"
                    class="h-10 w-10 rounded-full object-cover"
                  />
                  <div>
                    <div class="text-sm font-semibold text-(--foreground)">{{ t.name }}</div>
                    <div class="text-xs text-(--muted-foreground)">{{ t.role }}</div>
                  </div>
                </div>
              </div>
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== PRICING PLANS ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="text-center mb-14 scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-4">
            Tarifs
          </div>
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Des Offres Qui
            <span class="font-editorial-italic text-(--primary)">Évoluent Avec Vous</span>
          </h2>
          <p class="mt-4 mx-auto max-w-2xl text-(--muted-foreground)">
            Que vous lanciez votre activité ou que vous développiez votre produit, nous avons un plan adapté à votre étape — sans superflu.
          </p>
        </div>

        <div class="grid grid-cols-1 gap-6 sm:grid-cols-3">
          @for (plan of pricingPlans; track plan.name; let i = $index) {
            <div
              class="relative rounded-2xl border bg-(--card) p-6 sm:p-8 transition-all duration-300 hover:-translate-y-1 scroll-animate anim-fade-up"
              [class]="'relative rounded-2xl border bg-(--card) p-6 sm:p-8 transition-all duration-300 hover:-translate-y-1 scroll-animate anim-fade-up delay-' + (i + 1) + '00 ' + (plan.popular ? 'border-emerald-accent/50 shadow-lg shadow-emerald-accent/10' : 'border-(--border)')"
            >
              @if (plan.popular) {
                <div class="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full bg-(--primary) px-4 py-1 text-xs font-bold text-white">
                  Populaire
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
                </div>
              }
              <span class="text-xs font-mono tracking-wider text-(--muted-foreground)">{{ i + 1 }}</span>
              <h3 class="mt-2 text-lg font-bold text-(--foreground)">{{ plan.name }}</h3>
              <p class="mt-1 text-sm text-(--muted-foreground)">{{ plan.subtitle }}</p>

              <div class="mt-6">
                <span class="text-3xl sm:text-4xl font-bold text-(--foreground) font-editorial">{{ plan.price }}</span>
                <span class="text-sm text-(--muted-foreground) ml-1">/ Projet</span>
              </div>

              <a
                routerLink="/contact"
                class="mt-6 flex items-center justify-center gap-2 rounded-full px-6 py-3 text-sm font-semibold transition-colors cursor-pointer"
                [class]="plan.popular ? 'bg-(--primary) text-white hover:bg-emerald-hover' : 'border border-(--border) text-(--foreground) hover:bg-(--accent)'"
              >
                Commencer
              </a>

              <div class="mt-6 pt-4 border-t border-(--border)">
                <span class="text-xs font-medium text-(--muted-foreground) uppercase tracking-wider">Détails</span>
                <ul class="mt-3 space-y-2">
                  @for (feat of plan.features; track feat) {
                    <li class="flex items-start gap-2 text-sm text-(--muted-foreground)">
                      <lucide-icon [img]="CheckIcon" [size]="14" class="mt-0.5 text-emerald-500 shrink-0"></lucide-icon>
                      {{ feat }}
                    </li>
                  }
                </ul>
              </div>
            </div>
          }
        </div>
      </div>
    </section>

<<<<<<< HEAD
    <!-- ===== FAQ SECTION ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-24 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-5 gap-12">
          <!-- Left: Title -->
          <div class="lg:col-span-2 scroll-animate anim-fade-up">
            <span class="text-xs font-semibold tracking-[0.2em] uppercase text-(--primary) mb-3 block">
              Questions Fréquentes
            </span>
            <h2 class="font-display text-3xl sm:text-4xl font-bold text-(--foreground) leading-snug">
              Tout ce que vous devez
              <span class="italic font-light text-(--primary)">savoir</span>
            </h2>
            <p class="mt-4 text-sm text-(--muted-foreground)">
              Vous ne trouvez pas votre réponse ?
              <a href="mailto:lmp.assistance@gmail.com" class="text-(--primary) hover:underline">Écrivez-nous</a>
            </p>
          </div>

          <!-- Right: Accordion -->
          <div class="lg:col-span-3 space-y-3 scroll-animate anim-fade-up delay-200">
            @for (faq of faqs; track faq.question; let i = $index) {
              <div class="rounded-xl border border-(--border) bg-(--card) overflow-hidden transition-colors"
                   [class.border-emerald-500/20]="openFaq() === i">
                <button
                  class="flex w-full items-center justify-between px-6 py-4 text-left cursor-pointer"
                  (click)="toggleFaq(i)"
                >
                  <span class="text-sm font-semibold text-(--foreground) pr-4">{{ faq.question }}</span>
                  <lucide-icon
                    [img]="ChevronDownIcon"
                    [size]="16"
                    class="shrink-0 text-(--muted-foreground) transition-transform duration-300"
                    [class.rotate-180]="openFaq() === i"
                  ></lucide-icon>
                </button>
                @if (openFaq() === i) {
                  <div class="px-6 pb-4">
                    <p class="text-sm leading-relaxed text-(--muted-foreground)">{{ faq.answer }}</p>
                  </div>
                }
              </div>
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== CTA SECTION ===== -->
    <section class="relative overflow-hidden">
      <!-- Emerald gradient background -->
      <div class="absolute inset-0 bg-gradient-to-br from-emerald-600 via-emerald-500 to-teal-500"></div>
      <div
        class="pointer-events-none absolute inset-0 opacity-10"
        style="background-image: radial-gradient(circle, white 1px, transparent 1px); background-size: 32px 32px;"
      ></div>

      <div class="relative z-10 mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-2xl text-center scroll-animate anim-fade-up">
          <h2 class="font-display text-3xl sm:text-4xl lg:text-5xl font-bold text-white leading-tight">
            Prêt à transformer<br />
            <span class="italic font-light">votre visibilité ?</span>
          </h2>
          <p class="mt-4 text-white/70 text-base">
            Discutons de vos objectifs. L'audit initial est offert et sans engagement.
          </p>
          <div class="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
            <button
              (click)="showAppointment.set(true)"
              class="group inline-flex items-center gap-2 rounded-full bg-white px-7 py-3.5 text-sm font-semibold text-emerald-700 transition-all duration-300 hover:bg-white/90 hover:shadow-lg cursor-pointer"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver mon audit gratuit
              <lucide-icon [img]="ArrowRightIcon" [size]="16" class="transition-transform group-hover:translate-x-0.5"></lucide-icon>
            </button>
            <a
              href="mailto:lmp.assistance@gmail.com"
              class="inline-flex items-center gap-2 rounded-full border border-white/30 px-7 py-3.5 text-sm font-medium text-white transition-colors hover:bg-white/10 cursor-pointer"
            >
              Écrivez-nous →
            </a>
=======
    <!-- ===== FAQ ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="text-center mb-14 scroll-animate anim-fade-up">
          <div class="inline-flex items-center gap-2 rounded-full border border-emerald-accent/20 bg-emerald-accent/10 px-4 py-1.5 text-xs font-medium text-emerald-accent mb-4">
            FAQs
          </div>
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Questions
            <span class="font-editorial-italic text-(--primary)">Fréquentes</span>
          </h2>
        </div>

        <div class="space-y-3 scroll-animate anim-fade-up delay-100">
          @for (faq of faqs; track faq.question; let i = $index) {
            <div class="rounded-2xl border border-(--border) bg-(--card) overflow-hidden">
              <button
                class="flex w-full items-center justify-between p-5 text-left cursor-pointer"
                (click)="toggleFaq(i)"
              >
                <span class="text-sm font-medium text-(--foreground)">{{ faq.question }}</span>
                <lucide-icon
                  [img]="ChevronDownIcon"
                  [size]="18"
                  class="text-(--muted-foreground) transition-transform duration-200 shrink-0 ml-4"
                  [class.rotate-180]="openFaqIndex() === i"
                ></lucide-icon>
              </button>
              @if (openFaqIndex() === i) {
                <div class="px-5 pb-5 text-sm text-(--muted-foreground) leading-relaxed border-t border-(--border) pt-4">
                  {{ faq.answer }}
                </div>
              }
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== CTA + CONTACT FORM ===== -->
    <section class="border-b border-(--border) bg-(--card)/30">
      <div class="mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <!-- Left: CTA with dot pattern -->
          <div
            class="relative rounded-2xl border border-(--border) overflow-hidden flex items-center justify-center min-h-[400px] scroll-animate anim-fade-up"
          >
            <div
              class="absolute inset-0"
              style="background-image: radial-gradient(circle, var(--primary) 1px, transparent 1px); background-size: 24px 24px; opacity: 0.08;"
            ></div>
            <div class="relative text-center p-8">
              <h2 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-(--foreground) leading-tight">
                Parlons de
                <br />
                <span class="font-editorial-italic text-(--primary)">Votre Prochain Projet</span>
              </h2>
              <p class="mt-4 text-(--muted-foreground) max-w-md mx-auto">
                Discutons de vos objectifs. L'audit initial est offert et sans engagement.
              </p>
              <button
                (click)="showAppointment.set(true)"
                class="mt-8 inline-flex items-center gap-2 rounded-full bg-(--primary) px-6 py-3 text-sm font-semibold text-white transition-all hover:bg-emerald-hover hover:scale-105 cursor-pointer"
              >
                <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
                Réserver mon audit
              </button>
            </div>
          </div>

          <!-- Right: Contact form -->
          <div class="rounded-2xl border border-(--border) bg-(--card) p-6 sm:p-8 scroll-animate anim-fade-up delay-100">
            <h3 class="text-lg font-bold text-(--foreground) mb-6">Remplissez ce formulaire</h3>
            <form (ngSubmit)="onContactSubmit()" class="space-y-5">
              <div class="space-y-1.5">
                <label class="text-sm font-medium text-(--foreground)">Votre Nom</label>
                <input
                  type="text"
                  placeholder="Entrez votre nom complet"
                  [(ngModel)]="contactForm.name"
                  name="name"
                  required
                  class="w-full bg-transparent border-b border-(--border) py-3 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-(--primary) transition-colors"
                />
              </div>
              <div class="space-y-1.5">
                <label class="text-sm font-medium text-(--foreground)">Votre Email</label>
                <input
                  type="email"
                  placeholder="Entrez votre email"
                  [(ngModel)]="contactForm.email"
                  name="email"
                  required
                  class="w-full bg-transparent border-b border-(--border) py-3 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-(--primary) transition-colors"
                />
              </div>
              <div class="space-y-1.5">
                <label class="text-sm font-medium text-(--foreground)">Parlez-nous de votre projet</label>
                <textarea
                  placeholder="Décrivez votre projet et vos objectifs..."
                  [(ngModel)]="contactForm.message"
                  name="message"
                  required
                  rows="4"
                  class="w-full bg-transparent border-b border-(--border) py-3 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-(--primary) transition-colors resize-none"
                ></textarea>
              </div>

              @if (contactSuccess()) {
                <div class="rounded-lg bg-emerald-500/10 px-4 py-3 text-sm text-emerald-500">
                  {{ contactSuccess() }}
                </div>
              }
              @if (contactError()) {
                <div class="rounded-lg bg-red-500/10 px-4 py-3 text-sm text-red-400">
                  {{ contactError() }}
                </div>
              }

              <button
                type="submit"
                [disabled]="contactSubmitting()"
                class="w-full flex items-center justify-center gap-2 rounded-full bg-(--primary) px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-emerald-hover cursor-pointer disabled:opacity-50"
              >
                @if (contactSubmitting()) {
                  Envoi en cours...
                } @else {
                  <lucide-icon [img]="SendIcon" [size]="16"></lucide-icon>
                  Envoyer le message
                }
              </button>
            </form>
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
          </div>
        </div>
      </div>
    </section>

    <!-- Appointment Modal -->
    <lmp-appointment-modal
      [isOpen]="showAppointment()"
      (closed)="showAppointment.set(false)"
    />
  `,
})
export class HomeComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly catalogService = inject(CatalogService);
  private readonly seo = inject(SeoService);
  private readonly http = inject(HttpClient);

  // Lucide Icons
  readonly ArrowRightIcon = ArrowRight;
  readonly CheckIcon = Check;
<<<<<<< HEAD
  readonly CalendarIcon = Calendar;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly SearchIcon = Search;
  readonly BarChart3Icon = BarChart3;
  readonly GlobeIcon = Globe;
  readonly MegaphoneIcon = Megaphone;
  readonly TargetIcon = Target;
  readonly LayersIcon = Layers;
  readonly RocketIcon = Rocket;
  readonly ChevronDownIcon = ChevronDown;
  readonly QuoteIcon = Quote;
  readonly ShieldIcon = Shield;
  readonly UsersIcon = Users;
  readonly AwardIcon = Award;
  readonly ZapIcon = Zap;
  readonly TrendingUpIcon = TrendingUp;
  readonly StarIcon = Star;
=======
  readonly ChevronDownIcon = ChevronDown;
  readonly CalendarIcon = Calendar;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly SendIcon = Send;
  readonly ExternalLinkIcon = ExternalLink;
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a

  readonly showAppointment = signal(false);
  readonly featuredServices = signal<ServiceItem[]>([]);
  readonly loadingFeatured = signal(true);
  readonly totalFeaturedCount = signal(0);
<<<<<<< HEAD
  readonly openFaq = signal<number | null>(null);
  readonly testimonialPage = signal(0);
=======
  readonly openFaqIndex = signal<number | null>(null);

  // Contact form
  readonly contactSubmitting = signal(false);
  readonly contactSuccess = signal('');
  readonly contactError = signal('');
  readonly contactForm = { name: '', email: '', message: '', subject: 'Nouveau contact depuis le site', phone: '', company: '', consent: true };
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a

  private scrollObserver?: IntersectionObserver;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

<<<<<<< HEAD
  // ── Hero ──
  readonly heroTags = ['SEO', 'Google Ads', 'Web', 'Google My Business', 'Branding'];

  // ── Partner Ticker ──
  readonly partnerLogos = [
    'Google Partner',
    'Meta Business',
    'Stripe Certified',
    'Trustpilot Verified',
    'HubSpot Partner',
    'Semrush Certified',
    'Google Analytics',
    'WordPress VIP',
  ];

  // ── About Stats ──
  readonly aboutStats = [
    { value: '500+', label: 'Clients Satisfaits' },
    { value: '10+', label: "Années d'Expérience" },
    { value: '98%', label: 'Satisfaction Client' },
  ];

  // ── Process Steps ──
  readonly processSteps = [
    { icon: Search, title: 'Audit & Analyse', description: 'Nous analysons votre présence digitale actuelle et identifions les opportunités de croissance.' },
    { icon: Target, title: 'Stratégie Sur Mesure', description: "Un plan d'action personnalisé basé sur vos objectifs et votre marché spécifique." },
    { icon: Rocket, title: 'Exécution & Optimisation', description: 'Mise en œuvre technique avec suivi continu et ajustements pour maximiser les résultats.' },
    { icon: TrendingUp, title: 'Résultats & Croissance', description: 'Rapports transparents et croissance mesurable de votre visibilité en ligne.' },
  ];

  // ── Testimonials ──
  readonly testimonials = [
    {
      quote: 'LMP a complètement changé notre approche locale. En 3 mois, nos appels entrants ont doublé. Le tableau de bord est clair et le support ultra réactif.',
      name: 'Marie Lefevre',
      role: 'Restauratrice, Bruxelles',
      avatar: 'https://i.pravatar.cc/80?u=marie-lefevre',
    },
    {
      quote: "Le service de gestion des avis m'a sauvé un temps précieux. Ma note moyenne est passée de 3.8 à 4.7 en quelques semaines. Indispensable pour mon activité.",
      name: 'Thomas Dubois',
      role: 'Institut de Beauté, Liège',
      avatar: 'https://i.pravatar.cc/80?u=thomas-dubois',
    },
    {
      quote: "L'équipe technique est impressionnante. Ils ont réglé un problème de fiche suspendue en 48h que je traînais depuis des mois. Merci pour l'efficacité.",
      name: 'Claire Vandenberghe',
      role: 'Cabinet Dentaire, Namur',
      avatar: 'https://i.pravatar.cc/80?u=claire-vandenberghe',
    },
    {
      quote: "Depuis que LMP gère notre référencement, nous sommes premiers sur Google Maps dans notre zone. Le retour sur investissement est exceptionnel.",
      name: 'Antoine Mercier',
      role: 'Garage Automobile, Charleroi',
      avatar: 'https://i.pravatar.cc/80?u=antoine-mercier',
    },
    {
      quote: "La création de notre site web a dépassé toutes nos attentes. L'intégration avec notre profil Google est parfaite et les résultats sont immédiats.",
      name: 'Sophie Renard',
      role: 'Fleuriste, Waterloo',
      avatar: 'https://i.pravatar.cc/80?u=sophie-renard',
    },
    {
      quote: "Un accompagnement personnalisé qui fait vraiment la différence. Pas de robots, des vraies réponses à nos questions et une stratégie sur mesure.",
      name: 'David Claessens',
      role: 'Boulangerie, Uccle',
      avatar: 'https://i.pravatar.cc/80?u=david-claessens',
    },
  ];

  visibleTestimonials = signal(this.testimonials.slice(0, 3));

  prevTestimonial(): void {
    const page = this.testimonialPage();
    const newPage = page === 0 ? Math.ceil(this.testimonials.length / 3) - 1 : page - 1;
    this.testimonialPage.set(newPage);
    this.visibleTestimonials.set(this.testimonials.slice(newPage * 3, newPage * 3 + 3));
  }

  nextTestimonial(): void {
    const page = this.testimonialPage();
    const maxPage = Math.ceil(this.testimonials.length / 3) - 1;
    const newPage = page >= maxPage ? 0 : page + 1;
    this.testimonialPage.set(newPage);
    this.visibleTestimonials.set(this.testimonials.slice(newPage * 3, newPage * 3 + 3));
  }

  // ── FAQ ──
  readonly faqs = [
    {
      question: 'Combien de temps faut-il pour voir des résultats SEO ?',
      answer: 'Les premiers résultats sont généralement visibles entre 4 et 8 semaines, selon votre secteur et la concurrence locale. Nous fournissons des rapports détaillés pour suivre votre progression en temps réel.',
    },
    {
      question: 'Est-ce que la consultation initiale est vraiment gratuite ?',
      answer: "Oui, l'audit initial est 100% gratuit et sans engagement. Nous analysons votre présence digitale actuelle et vous présentons les opportunités d'amélioration avec un plan d'action concret.",
    },
    {
      question: 'Quels types d\'entreprises accompagnez-vous ?',
      answer: 'Nous accompagnons tous types d\'entreprises locales : restaurants, commerces, cabinets médicaux, garages, instituts de beauté, artisans, et bien d\'autres. Notre expertise couvre toute la Belgique et l\'Europe francophone.',
    },
    {
      question: 'Comment fonctionne le paiement ?',
      answer: 'Nous proposons des paiements sécurisés via Stripe. Vous pouvez choisir un paiement unique ou un abonnement mensuel selon le service. Aucun frais caché — tout est transparent dès le départ.',
    },
    {
      question: 'Puis-je annuler mon abonnement à tout moment ?',
      answer: "Oui, tous nos abonnements sont sans engagement de durée. Vous pouvez annuler à tout moment depuis votre tableau de bord ou en nous contactant directement.",
    },
  ];

  toggleFaq(index: number): void {
    this.openFaq.set(this.openFaq() === index ? null : index);
  }

  // ── Fallback Services ──
  readonly fallbackServices = [
    {
      subtitle: 'Sécurisations et Accès Google My Business',
      description: 'Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti.',
      price: '353,89 €',
    },
    {
      subtitle: 'Référencement Optimale VIP+',
      description: 'Service premium exclusif avec garantie de résultats exceptionnels et mots clés garantis.',
      price: '750,79 €',
    },
    {
      subtitle: 'Gestion des Avis',
      description: 'Améliorez votre réputation en ligne. Soyez le mieux noté de votre secteur.',
      price: '747,43 €',
    },
    {
      subtitle: 'Création Site Web',
      description: 'Site web professionnel avec protocole SSL et référencement optimisé.',
      price: '550,00 €',
    },
=======
  // Particles for hero background
  readonly particles = Array.from({ length: 10 }, () => ({
    x: Math.random() * 90 + 5,
    y: Math.random() * 80 + 10,
    size: Math.random() * 3 + 2,
    duration: Math.random() * 4 + 4,
    delay: Math.random() * 3,
  }));

  readonly serviceTags = ['SEO Local', 'Google Ads', 'Création Web', 'Branding', 'Google My Business', 'Gestion des Avis'];

  readonly partners = [
    { name: 'Google Partner', icon: '🔍' },
    { name: 'Meta Business', icon: '📘' },
    { name: 'Stripe Payments', icon: '💳' },
    { name: 'HubSpot', icon: '🟠' },
    { name: 'Trustpilot', icon: '⭐' },
    { name: 'Semrush', icon: '📊' },
    { name: 'Google Analytics', icon: '📈' },
    { name: 'WordPress', icon: '🌐' },
    { name: 'Google Partner', icon: '🔍' },
    { name: 'Meta Business', icon: '📘' },
    { name: 'Stripe Payments', icon: '💳' },
    { name: 'HubSpot', icon: '🟠' },
    { name: 'Trustpilot', icon: '⭐' },
    { name: 'Semrush', icon: '📊' },
    { name: 'Google Analytics', icon: '📈' },
    { name: 'WordPress', icon: '🌐' },
  ];

  readonly coreServices = [
    { icon: '📍', title: 'Référencement Local', description: 'Optimisez votre fiche Google My Business et dominez les résultats de recherche locale pour attirer plus de clients.' },
    { icon: '🔍', title: 'Optimisation SEO', description: 'Améliorez votre positionnement sur les moteurs de recherche avec des stratégies SEO data-driven et mesurables.' },
    { icon: '💻', title: 'Développement Web', description: 'Créez des sites web performants, responsives et optimisés pour la conversion avec les dernières technologies.' },
    { icon: '📢', title: 'Marketing Digital', description: 'Lancez des campagnes publicitaires ciblées sur Google Ads, Meta et d\'autres plateformes pour maximiser votre ROI.' },
  ];

  readonly stats = [
    { value: '140+', label: 'Clients Accompagnés', sublabel: 'en Europe' },
    { value: '10+', label: 'Années d\'Expérience', sublabel: 'depuis 2016' },
    { value: '98%', label: 'Taux de Satisfaction', sublabel: 'clients fidèles' },
  ];

  readonly processSteps = [
    { title: 'Audit', description: 'Analyse complète de votre présence digitale actuelle et identification des opportunités d\'amélioration.' },
    { title: 'Stratégie', description: 'Élaboration d\'un plan d\'action personnalisé aligné sur vos objectifs business et votre budget.' },
    { title: 'Exécution', description: 'Mise en œuvre de solutions performantes avec des résultats mesurables dès les premières semaines.' },
    { title: 'Résultats & Croissance', description: 'Suivi continu, optimisation et scaling pour une croissance durable et maîtrisée.' },
  ];

  readonly certifications = [
    { source: 'Google', title: 'Partenaire Certifié Google Ads & Search', project: 'Google Partner Premium' },
    { source: 'Meta', title: 'Gestionnaire Certifié Meta Business Suite', project: 'Publicité Facebook & Instagram' },
    { source: 'SEO Expert', title: 'Certification Référencement Naturel Avancé', project: 'Audit & Optimisation SEO' },
    { source: 'Google Maps', title: 'Spécialiste Google My Business Vérifié', project: 'Référencement Local 360°' },
  ];

  readonly testimonials = [
    {
      quote: 'LMP a complètement transformé notre visibilité en ligne. En seulement 3 mois, nos appels entrants ont doublé et notre chiffre d\'affaires local a augmenté de 40%. Leur approche data-driven est exactement ce qu\'il nous fallait.',
      name: 'Marie Dubois',
      role: 'CEO, Brasserie Belge — Bruxelles',
    },
    {
      quote: 'Le service de gestion des avis m\'a fait gagner un temps précieux. Ma note est passée de 3.8 à 4.7 étoiles. Indispensable.',
      name: 'Antoine Leroy',
      role: 'Directeur, Institut de Beauté — Liège',
    },
    {
      quote: 'L\'équipe technique a résolu un problème de fiche suspendue en 48h que je traînais depuis des mois. Efficacité remarquable.',
      name: 'Isabelle Fontaine',
      role: 'Gérante, Garage Auto — Namur',
    },
    {
      quote: 'Nous avons vu une augmentation de 60% du trafic en boutique après le lancement de notre campagne Google Ads avec LMP.',
      name: 'Thomas Renard',
      role: 'Fondateur, Concept Store — Anvers',
    },
  ];

  readonly pricingPlans = [
    {
      name: 'Starter',
      subtitle: 'Pour les projets de démarrage',
      price: '353€+',
      popular: false,
      features: [
        'Audit & diagnostic initial',
        'Sécurisation Google My Business',
        'Configuration de base',
        'Support email',
      ],
    },
    {
      name: 'Growth',
      subtitle: 'Pour les marques en croissance',
      price: '750€+',
      popular: true,
      features: [
        'Référencement local complet',
        'Gestion des avis en ligne',
        'SEO on-page optimisé',
        'Rapports mensuels détaillés',
        'Support prioritaire',
      ],
    },
    {
      name: 'Custom',
      subtitle: 'Pour les projets complexes',
      price: 'Sur mesure',
      popular: false,
      features: [
        'Solution complète sur mesure',
        'Création site web + SEO',
        'Campagnes Google Ads & Meta',
        'Équipe dédiée & chef de projet',
      ],
    },
  ];

  readonly faqs = [
    {
      question: 'Combien de temps faut-il pour voir des résultats ?',
      answer: 'Les premiers résultats sont généralement visibles entre 2 et 4 semaines pour le référencement local, et entre 3 et 6 mois pour le SEO naturel. Nous fournissons des rapports réguliers pour suivre l\'évolution.',
    },
    {
      question: 'Proposez-vous une consultation gratuite ?',
      answer: 'Oui, nous offrons un audit initial gratuit et sans engagement. Nous analysons votre présence en ligne et identifions les opportunités d\'amélioration concrètes.',
    },
    {
      question: 'Quels types d\'entreprises accompagnez-vous ?',
      answer: 'Nous accompagnons tous types d\'entreprises : commerces locaux, artisans, professions libérales, PME et grandes entreprises. Notre expertise couvre tous les secteurs d\'activité.',
    },
    {
      question: 'Proposez-vous un suivi des performances ?',
      answer: 'Absolument. Chaque client bénéficie d\'un tableau de bord personnalisé avec des rapports détaillés sur les performances, le trafic, les conversions et le ROI de chaque action.',
    },
    {
      question: 'Comment fonctionne le paiement ?',
      answer: 'Nous proposons des paiements sécurisés via Stripe. Vous pouvez choisir entre un paiement unique ou un abonnement mensuel selon le service choisi. Pas de frais cachés.',
    },
  ];

  readonly fallbackServices = [
    { emoji: '📍', category: 'Référencement Local', subtitle: 'Sécurisations et Accès Google My Business', description: 'Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti.', price: '353,89 €' },
    { emoji: '⭐', category: 'Référencement Premium', subtitle: 'Référencement Optimale VIP+', description: 'Service premium exclusif avec garantie de résultats exceptionnels.', price: '750,79 €' },
    { emoji: '💬', category: 'Réputation en Ligne', subtitle: 'Gestion des Avis', description: 'Améliorez votre réputation en ligne avec notre service de gestion des avis.', price: '747,43 €' },
    { emoji: '🏪', category: 'Marketing Local', subtitle: 'Présence Locales', description: 'Boostez votre visibilité locale avec une présence optimisée.', price: '2 200,00 €' },
    { emoji: '💻', category: 'Développement Web', subtitle: 'Création Site Web', description: 'Création de site web professionnel avec protocole SSL, référencement optimisé.', price: '550,00 €' },
    { emoji: '🚀', category: 'Google Premium', subtitle: 'Mise à jour 2026', description: 'Solution complète intégrant tous nos services premium.', price: '1 000,00 €' },
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
  ] as const;

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'LMP Digital Services — Marketing Digital & Référencement Local',
      description: 'Propulsez votre visibilité au sommet. Expertise en marketing digital, référencement SEO, Google My Business et création de sites web. 140+ clients satisfaits.',
      url: '/',
      keywords: 'marketing digital, référencement SEO, Google My Business, création site web, publicité en ligne, LMP, référencement local, Belgique, Bruxelles',
    });
    this.loadFeaturedServices();
  }

  private loadFeaturedServices(): void {
    this.loadingFeatured.set(true);
    this.catalogService.getFeaturedServices().subscribe({
      next: (services) => {
        const activeServices = services.filter(s => s.active);
        this.totalFeaturedCount.set(activeServices.length);
        this.featuredServices.set(activeServices.slice(0, 6));
        this.loadingFeatured.set(false);
        if (this.isBrowser) {
          setTimeout(() => this.reobserveAnimations(), 50);
        }
      },
      error: () => {
        this.loadingFeatured.set(false);
        if (this.isBrowser) {
          setTimeout(() => {
            document.querySelectorAll('.scroll-animate:not(.animate-visible)').forEach((el) => {
              this.scrollObserver?.observe(el);
            });
          }, 50);
        }
      },
    });
  }

<<<<<<< HEAD
=======
  toggleFaq(index: number): void {
    this.openFaqIndex.set(this.openFaqIndex() === index ? null : index);
  }

  onContactSubmit(): void {
    if (!this.contactForm.name || !this.contactForm.email || !this.contactForm.message) {
      this.contactError.set('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    this.contactSubmitting.set(true);
    this.contactError.set('');
    this.contactSuccess.set('');

    this.http
      .post(`${environment.apiUrl}/api/v1/contact`, this.contactForm, { withCredentials: true })
      .subscribe({
        next: () => {
          this.contactSuccess.set('Message envoyé avec succès ! Nous vous répondrons dans les 24h.');
          this.contactForm.name = '';
          this.contactForm.email = '';
          this.contactForm.message = '';
          this.contactSubmitting.set(false);
        },
        error: () => {
          this.contactError.set('Une erreur est survenue. Veuillez réessayer.');
          this.contactSubmitting.set(false);
        },
      });
  }

>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
  ngAfterViewInit(): void {
    if (!this.isBrowser) return;
    this.setupScrollObserver();
  }

<<<<<<< HEAD
    // Scroll animation observer
=======
  private setupScrollObserver(): void {
    this.scrollObserver?.disconnect();
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
    this.scrollObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('animate-visible');
          }
        });
      },
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' },
    );
    document.querySelectorAll('.scroll-animate').forEach((el) => {
      this.scrollObserver!.observe(el);
    });
<<<<<<< HEAD
=======
  }

  private reobserveAnimations(): void {
    document.querySelectorAll('.scroll-animate:not(.animate-visible)').forEach((el) => {
      this.scrollObserver?.observe(el);
    });
>>>>>>> ba3a7a58384d392171b1b7060ad4c5cdc460252a
  }

  ngOnDestroy(): void {
    this.scrollObserver?.disconnect();
  }
}
