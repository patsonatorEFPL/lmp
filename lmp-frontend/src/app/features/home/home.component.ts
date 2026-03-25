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
import { environment } from '../../../environments/environment';
import {
  LucideAngularModule,
  LucideIconData,
  ArrowRight,
  Check,
  ChevronDown,
  Calendar,
  ShoppingCart,
  Send,
  ExternalLink,
  MapPin,
  Search,
  Globe,
  Megaphone,
  Star,
  MessageSquare,
  Store,
  Rocket,
  CreditCard,
  BarChart2,
  TrendingUp,
  Users,
} from 'lucide-angular';
import { AppointmentModalComponent } from '../../shared/modals/appointment-modal.component';
import { CatalogService, ServiceItem } from '../../core/services/catalog.service';

@Component({
  selector: 'lmp-home',
  standalone: true,
  imports: [RouterLink, FormsModule, LucideAngularModule, AppointmentModalComponent, CurrencyPipe],
  template: `
    <!-- ===== HERO SECTION (includes ticker at bottom) ===== -->
    <section
      class="relative overflow-hidden -mt-14 pt-14 flex flex-col min-h-screen"
    >
      <!-- Background image with simple dark overlay -->
      <div class="absolute inset-0">
        <img
          src="/images/hero-bg-office.jpg"
          alt="Modern corporate office"
          class="absolute inset-0 w-full h-full object-cover"
        />
        <div class="absolute inset-0 bg-black/80"></div>
      </div>

      <!-- Hero content — grows to fill available space, centered -->
      <div class="relative z-10 mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 w-full flex-1 flex items-center py-20">
        <div class="max-w-2xl">
          <h1 class="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-white leading-tight">
            Propulsez votre visibilité digitale
          </h1>
          <p class="mt-4 text-base sm:text-lg text-white/60 leading-relaxed max-w-lg">
            Nous bâtissons des stratégies digitales, sites web et expériences
            numériques avec précision, clarté et engagement.
          </p>
          <div class="mt-8 flex items-center gap-4">
            <button
              (click)="showAppointment.set(true)"
              class="inline-flex items-center gap-2 rounded-sm bg-white px-5 py-2.5 text-sm font-medium text-gray-900 transition-colors hover:bg-gray-100 cursor-pointer"
            >
              Réserver un audit gratuit
            </button>
            <a
              routerLink="/services"
              class="inline-flex items-center gap-1.5 text-sm font-medium text-white/70 transition-colors hover:text-white cursor-pointer"
            >
              Voir les services
              <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
            </a>
          </div>
        </div>
      </div>

      <!-- Partner ticker — pinned at the bottom of the hero -->
      <div class="relative z-10 border-t border-white/10 overflow-hidden">
        <div class="py-4">
          <div class="ticker-track">
            @for (i of [0,1]; track i) {
              <div class="flex items-center gap-10 px-5">
                @for (partner of partners; track $index) {
                  <span class="text-sm text-white/40 whitespace-nowrap hover:text-white/80 transition-colors">
                    {{ partner.name }}
                  </span>
                }
              </div>
            }
          </div>
        </div>
      </div>
    </section>

    <!-- ===== SERVICES: WHAT WE DO ===== -->
    <section id="service" class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between mb-12">
          <div class="scroll-animate">
            <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
              Ce que nous faisons
            </h2>
          </div>
          <p class="max-w-lg text-sm text-(--muted-foreground) scroll-animate">
            Des solutions digitales de l'idée au lancement — alliant stratégie,
            design et technologie pour bâtir des résultats qui performent.
          </p>
        </div>

        <div class="grid grid-cols-1 gap-px rounded-sm border border-(--border) overflow-hidden sm:grid-cols-2">
          @for (svc of coreServices; track svc.title; let i = $index) {
            <div
              class="bg-(--card) p-8 sm:p-10 transition-colors duration-150 hover:bg-(--accent) scroll-animate"
              [style.transition-delay.ms]="(i + 1) * 100"
            >
              <div class="flex items-center justify-center h-12 w-12 rounded-sm bg-(--muted) mb-5">
                <lucide-icon [img]="getIcon(svc.iconName)" [size]="20" class="text-(--muted-foreground)"></lucide-icon>
              </div>
              <h3 class="text-base font-semibold text-(--foreground)">{{ svc.title }}</h3>
              <p class="mt-2 text-sm leading-relaxed text-(--muted-foreground)">{{ svc.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== ABOUT + STATS ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="scroll-animate">
          <p class="max-w-3xl text-xl sm:text-2xl font-light leading-relaxed text-(--foreground)">
            Nous aidons les entreprises ambitieuses à bâtir une présence digitale qui se démarque et qui convertit.
          </p>
        </div>

        <div class="mt-12 grid grid-cols-1 gap-8 sm:grid-cols-3">
          @for (stat of stats; track stat.label; let i = $index) {
            <div
              class="scroll-animate"
              [style.transition-delay.ms]="(i + 1) * 100"
            >
              <div class="text-4xl sm:text-5xl font-bold text-(--foreground)">{{ stat.value }}</div>
              <div class="mt-1 text-sm font-medium text-(--foreground)">{{ stat.label }}</div>
              <div class="text-xs text-(--muted-foreground)">{{ stat.sublabel }}</div>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== FEATURED SERVICES SHOWCASE ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="mb-10 scroll-animate">
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Solutions
          </h2>
          <p class="mt-3 max-w-2xl text-sm text-(--muted-foreground)">
            Une gamme complète d'outils et de services pour dominer votre marché local et national.
          </p>
        </div>

        <!-- Loading skeleton -->
        @if (loadingFeatured()) {
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            @for (s of [1,2,3,4,5,6]; track s) {
              <div class="rounded-sm border border-(--border) bg-(--card) p-5 animate-pulse">
                <div class="flex items-start justify-between mb-3">
                  <div class="h-10 w-10 rounded-sm bg-(--muted)"></div>
                  <div class="h-4 w-16 rounded-sm bg-(--muted)"></div>
                </div>
                <div class="h-4 w-3/4 rounded-sm bg-(--muted)"></div>
                <div class="mt-2 space-y-1.5">
                  <div class="h-3 w-full rounded-sm bg-(--muted)"></div>
                  <div class="h-3 w-5/6 rounded-sm bg-(--muted)"></div>
                </div>
              </div>
            }
          </div>
        }

        <!-- Service cards from API -->
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          @for (service of featuredServices(); track service.id; let i = $index) {
            <a
              routerLink="/services"
              [fragment]="service.slug"
              class="group flex flex-col rounded-sm border border-(--border) bg-(--card) p-5 transition-colors duration-150 hover:border-(--primary)/30 scroll-animate cursor-pointer"
              [style.transition-delay.ms]="(i % 3 + 1) * 100"
            >
              <div class="flex items-start justify-between mb-3">
                <div class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted)">
                  <lucide-icon [img]="getIcon(service.icon)" [size]="18" class="text-(--muted-foreground)"></lucide-icon>
                </div>
                <span class="rounded-sm bg-(--muted) px-2 py-0.5 text-xs font-medium text-(--muted-foreground)">
                  {{ service.categoryName }}
                </span>
              </div>
              <h3 class="text-sm font-semibold text-(--foreground)">{{ service.title }}</h3>
              <p class="mt-1.5 flex-1 text-sm leading-relaxed text-(--muted-foreground)">{{ service.description }}</p>
              <ul class="mt-3 space-y-1">
                @for (benefit of service.benefits?.slice(0, 3); track benefit) {
                  <li class="flex items-start gap-2 text-xs text-(--muted-foreground)">
                    <lucide-icon [img]="CheckIcon" [size]="12" class="mt-0.5 text-(--primary) shrink-0"></lucide-icon>
                    {{ benefit }}
                  </li>
                }
              </ul>
              <div class="mt-4 flex items-end justify-between border-t border-(--border) pt-3">
                <div>
                  @if (service.currentOffer) {
                    <span class="text-lg font-bold text-(--foreground)">
                      {{ service.currentOffer.price | currency:'EUR':'symbol':'1.2-2':'fr' }}
                    </span>
                    <span class="text-xs text-(--muted-foreground) ml-1">
                      {{ service.currentOffer.durationType === 'ONE_TIME' ? 'unique' :
                         service.currentOffer.durationType === 'MONTHLY' ? '/ mois' :
                         service.currentOffer.durationType === 'YEARLY' ? '/ an' : '' }}
                    </span>
                  } @else {
                    <span class="text-base font-semibold text-(--foreground)">Sur devis</span>
                  }
                </div>
                <div class="flex h-7 w-7 items-center justify-center rounded-sm bg-(--muted) text-(--muted-foreground) transition-colors group-hover:text-(--primary)">
                  <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
                </div>
              </div>
            </a>
          }
        </div>

        @if (featuredServices().length === 0 && !loadingFeatured()) {
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            @for (service of fallbackServices; track service.subtitle) {
              <a
                routerLink="/services"
                class="group flex flex-col rounded-sm border border-(--border) bg-(--card) p-5 transition-colors duration-150 hover:border-(--primary)/30 scroll-animate cursor-pointer"
              >
                <div class="flex items-start justify-between mb-3">
                  <div class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted)">
                    <lucide-icon [img]="getIcon(service.iconName)" [size]="18" class="text-(--muted-foreground)"></lucide-icon>
                  </div>
                  <span class="rounded-sm bg-(--muted) px-2 py-0.5 text-xs font-medium text-(--muted-foreground)">
                    {{ service.category }}
                  </span>
                </div>
                <h3 class="text-sm font-semibold text-(--foreground)">{{ service.subtitle }}</h3>
                <p class="mt-1.5 flex-1 text-sm leading-relaxed text-(--muted-foreground)">{{ service.description }}</p>
                <div class="mt-4 flex items-end justify-between border-t border-(--border) pt-3">
                  <span class="text-lg font-bold text-(--foreground)">{{ service.price }}</span>
                  <div class="flex h-7 w-7 items-center justify-center rounded-sm bg-(--muted) text-(--muted-foreground)">
                    <lucide-icon [img]="ArrowRightIcon" [size]="14"></lucide-icon>
                  </div>
                </div>
              </a>
            }
          </div>
        }

        <div class="mt-8 scroll-animate">
          <a
            routerLink="/services"
            class="inline-flex items-center gap-2 rounded-sm border border-(--border) bg-(--card) px-5 py-2.5 text-sm font-medium text-(--foreground) transition-colors hover:bg-(--accent) cursor-pointer"
          >
            Voir tous les services
            @if (totalFeaturedCount() > 6) {
              <span class="text-xs text-(--muted-foreground)">(+{{ totalFeaturedCount() - 6 }} autres)</span>
            }
          </a>
        </div>
      </div>
    </section>

    <!-- ===== PROCESS SECTION ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between mb-12">
          <div class="scroll-animate">
            <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
              Notre processus
            </h2>
          </div>
          <p class="max-w-lg text-sm text-(--muted-foreground) scroll-animate">
            Nous gardons les choses simples et collaboratives — pour passer du concept au lancement sans chaos.
          </p>
        </div>

        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          @for (step of processSteps; track step.title; let i = $index) {
            <div
              class="rounded-sm border border-(--border) bg-(--card) p-5 scroll-animate"
              [style.transition-delay.ms]="(i + 1) * 100"
            >
              <span class="text-xs font-mono text-(--muted-foreground)">{{ (i + 1).toString().padStart(2, '0') }}</span>
              <h3 class="mt-2 text-base font-semibold text-(--foreground)">{{ step.title }}</h3>
              <p class="mt-2 text-sm leading-relaxed text-(--muted-foreground)">{{ step.description }}</p>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== CERTIFICATIONS ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="mb-10 scroll-animate">
          <h2 class="text-2xl font-bold text-(--foreground)">Certifications</h2>
        </div>

        <div class="scroll-animate">
          @for (cert of certifications; track cert.title) {
            <div class="flex flex-col sm:flex-row sm:items-center justify-between py-4 border-b border-(--border) last:border-b-0 gap-2 sm:gap-4">
              <span class="text-sm text-(--muted-foreground) sm:w-32 shrink-0">{{ cert.source }}</span>
              <h3 class="text-sm font-semibold text-(--foreground) flex-1">{{ cert.title }}</h3>
              <span class="text-xs text-(--muted-foreground) sm:w-48 text-right shrink-0">{{ cert.project }}</span>
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== TESTIMONIALS ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="mb-10 scroll-animate">
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Ce que disent nos clients
          </h2>
        </div>

        <div class="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <!-- Large featured testimonial -->
          <div class="lg:col-span-2 rounded-sm border border-(--border) bg-(--card) p-6 sm:p-8 scroll-animate">
            <p class="text-base sm:text-lg leading-relaxed text-(--foreground)">
              "{{ testimonials[0].quote }}"
            </p>
            <div class="mt-6 flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-sm bg-(--muted) text-sm font-semibold text-(--foreground) shrink-0">
                {{ getInitials(testimonials[0].name) }}
              </div>
              <div>
                <div class="text-sm font-medium text-(--foreground)">{{ testimonials[0].name }}</div>
                <div class="text-xs text-(--muted-foreground)">{{ testimonials[0].role }}</div>
              </div>
            </div>
          </div>

          <!-- Smaller testimonials -->
          <div class="flex flex-col gap-4">
            @for (t of testimonials.slice(1); track t.name; let i = $index) {
              <div
                class="rounded-sm border border-(--border) bg-(--card) p-5 scroll-animate"
                [style.transition-delay.ms]="(i + 1) * 100"
              >
                <p class="text-sm leading-relaxed text-(--muted-foreground)">
                  "{{ t.quote }}"
                </p>
                <div class="mt-3 flex items-center gap-2">
                  <div class="flex h-8 w-8 items-center justify-center rounded-sm bg-(--muted) text-xs font-semibold text-(--foreground) shrink-0">
                    {{ getInitials(t.name) }}
                  </div>
                  <div>
                    <div class="text-xs font-medium text-(--foreground)">{{ t.name }}</div>
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
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="mb-10 scroll-animate">
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Tarifs
          </h2>
          <p class="mt-3 max-w-2xl text-sm text-(--muted-foreground)">
            Que vous lanciez votre activité ou que vous développiez votre produit, nous avons un plan adapté.
          </p>
        </div>

        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          @for (plan of pricingPlans; track plan.name; let i = $index) {
            <div
              class="relative rounded-sm border bg-(--card) p-6 scroll-animate"
              [class]="plan.popular ? 'border-(--primary) relative rounded-sm bg-(--card) p-6 scroll-animate' : 'border-(--border) relative rounded-sm bg-(--card) p-6 scroll-animate'"
              [style.transition-delay.ms]="(i + 1) * 100"
            >
              @if (plan.popular) {
                <div class="absolute -top-2.5 left-4 rounded-sm bg-(--primary) px-2.5 py-0.5 text-xs font-medium text-(--primary-foreground)">
                  Populaire
                </div>
              }
              <h3 class="text-base font-semibold text-(--foreground)">{{ plan.name }}</h3>
              <p class="mt-1 text-xs text-(--muted-foreground)">{{ plan.subtitle }}</p>

              <div class="mt-5">
                <span class="text-3xl font-bold text-(--foreground)">{{ plan.price }}</span>
                <span class="text-sm text-(--muted-foreground) ml-1">/ Projet</span>
              </div>

              <a
                routerLink="/contact"
                class="mt-5 flex items-center justify-center gap-2 rounded-sm px-5 py-2.5 text-sm font-medium transition-colors cursor-pointer"
                [class]="plan.popular ? 'bg-(--primary) text-(--primary-foreground) hover:opacity-90' : 'border border-(--border) text-(--foreground) hover:bg-(--accent)'"
              >
                Commencer
              </a>

              <div class="mt-5 pt-4 border-t border-(--border)">
                <ul class="space-y-2">
                  @for (feat of plan.features; track feat) {
                    <li class="flex items-start gap-2 text-sm text-(--muted-foreground)">
                      <lucide-icon [img]="CheckIcon" [size]="14" class="mt-0.5 text-(--primary) shrink-0"></lucide-icon>
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

    <!-- ===== FAQ ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="mb-10 scroll-animate">
          <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground)">
            Questions fréquentes
          </h2>
        </div>

        <div class="space-y-2 scroll-animate">
          @for (faq of faqs; track faq.question; let i = $index) {
            <div class="rounded-sm border border-(--border) bg-(--card) overflow-hidden">
              <button
                class="flex w-full items-center justify-between p-4 text-left cursor-pointer"
                (click)="toggleFaq(i)"
              >
                <span class="text-sm font-medium text-(--foreground)">{{ faq.question }}</span>
                <lucide-icon
                  [img]="ChevronDownIcon"
                  [size]="16"
                  class="text-(--muted-foreground) transition-transform duration-150 shrink-0 ml-4"
                  [class.rotate-180]="openFaqIndex() === i"
                ></lucide-icon>
              </button>
              @if (openFaqIndex() === i) {
                <div class="px-4 pb-4 text-sm text-(--muted-foreground) leading-relaxed border-t border-(--border) pt-3">
                  {{ faq.answer }}
                </div>
              }
            </div>
          }
        </div>
      </div>
    </section>

    <!-- ===== CTA + CONTACT FORM ===== -->
    <section class="border-b border-(--border)">
      <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <!-- Left: CTA -->
          <div class="flex flex-col justify-center scroll-animate">
            <h2 class="text-3xl sm:text-4xl font-bold text-(--foreground) leading-tight">
              Parlons de votre prochain projet
            </h2>
            <p class="mt-3 text-sm text-(--muted-foreground) max-w-md">
              Discutons de vos objectifs. L'audit initial est offert et sans engagement.
            </p>
            <button
              (click)="showAppointment.set(true)"
              class="mt-6 inline-flex items-center gap-2 rounded-sm bg-(--primary) px-5 py-2.5 text-sm font-medium text-(--primary-foreground) transition-colors hover:opacity-90 cursor-pointer w-fit"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver mon audit
            </button>
          </div>

          <!-- Right: Contact form -->
          <div class="rounded-sm border border-(--border) bg-(--card) p-6 scroll-animate">
            <h3 class="text-base font-semibold text-(--foreground) mb-5">Formulaire rapide</h3>
            <form (ngSubmit)="onContactSubmit()" class="space-y-4">
              <div>
                <label class="text-sm font-medium text-(--foreground)">Votre Nom</label>
                <input
                  type="text"
                  placeholder="Nom complet"
                  [(ngModel)]="contactForm.name"
                  name="name"
                  required
                  class="mt-1 w-full rounded-sm border border-(--border) bg-transparent px-3 py-2 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-(--primary) transition-colors"
                />
              </div>
              <div>
                <label class="text-sm font-medium text-(--foreground)">Votre Email</label>
                <input
                  type="email"
                  placeholder="email&#64;entreprise.be"
                  [(ngModel)]="contactForm.email"
                  name="email"
                  required
                  class="mt-1 w-full rounded-sm border border-(--border) bg-transparent px-3 py-2 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-(--primary) transition-colors"
                />
              </div>
              <div>
                <label class="text-sm font-medium text-(--foreground)">Votre projet</label>
                <textarea
                  placeholder="Décrivez votre projet..."
                  [(ngModel)]="contactForm.message"
                  name="message"
                  required
                  rows="3"
                  class="mt-1 w-full rounded-sm border border-(--border) bg-transparent px-3 py-2 text-sm text-(--foreground) placeholder:text-(--muted-foreground) outline-none focus:border-(--primary) transition-colors resize-none"
                ></textarea>
              </div>

              @if (contactSuccess()) {
                <div class="rounded-sm bg-(--success)/10 px-3 py-2 text-sm text-(--success)">
                  {{ contactSuccess() }}
                </div>
              }
              @if (contactError()) {
                <div class="rounded-sm bg-(--destructive)/10 px-3 py-2 text-sm text-(--destructive)">
                  {{ contactError() }}
                </div>
              }

              <button
                type="submit"
                [disabled]="contactSubmitting()"
                class="w-full flex items-center justify-center gap-2 rounded-sm bg-(--primary) px-5 py-2.5 text-sm font-medium text-(--primary-foreground) transition-colors hover:opacity-90 cursor-pointer disabled:opacity-50"
              >
                @if (contactSubmitting()) {
                  Envoi en cours...
                } @else {
                  <lucide-icon [img]="SendIcon" [size]="16"></lucide-icon>
                  Envoyer le message
                }
              </button>
            </form>
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
  readonly ChevronDownIcon = ChevronDown;
  readonly CalendarIcon = Calendar;
  readonly ShoppingCartIcon = ShoppingCart;
  readonly SendIcon = Send;
  readonly ExternalLinkIcon = ExternalLink;

  readonly iconMap: Record<string, LucideIconData> = {
    mapPin: MapPin,
    search: Search,
    globe: Globe,
    megaphone: Megaphone,
    star: Star,
    messageSquare: MessageSquare,
    store: Store,
    rocket: Rocket,
    creditCard: CreditCard,
    barChart2: BarChart2,
    trendingUp: TrendingUp,
    users: Users,
  };

  getIcon(name: string): LucideIconData {
    return this.iconMap[name] ?? Globe;
  }

  getInitials(name: string): string {
    return name.split(' ').slice(0, 2).map(n => n[0]).join('').toUpperCase();
  }

  readonly showAppointment = signal(false);
  readonly featuredServices = signal<ServiceItem[]>([]);
  readonly loadingFeatured = signal(true);
  readonly totalFeaturedCount = signal(0);
  readonly openFaqIndex = signal<number | null>(null);

  // Contact form
  readonly contactSubmitting = signal(false);
  readonly contactSuccess = signal('');
  readonly contactError = signal('');
  readonly contactForm = { name: '', email: '', message: '', subject: 'Nouveau contact depuis le site', phone: '', company: '', consent: true };

  private scrollObserver?: IntersectionObserver;
  private isBrowser: boolean;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  readonly partners = [
    { name: 'Google Partner' },
    { name: 'Meta Business' },
    { name: 'Stripe Payments' },
    { name: 'HubSpot' },
    { name: 'Trustpilot' },
    { name: 'Semrush' },
    { name: 'Google Analytics' },
    { name: 'WordPress' },
    { name: 'Google Partner' },
    { name: 'Meta Business' },
    { name: 'Stripe Payments' },
    { name: 'HubSpot' },
    { name: 'Trustpilot' },
    { name: 'Semrush' },
    { name: 'Google Analytics' },
    { name: 'WordPress' },
  ];

  readonly coreServices = [
    { iconName: 'mapPin', title: 'Référencement Local', description: 'Optimisez votre fiche Google My Business et dominez les résultats de recherche locale pour attirer plus de clients.' },
    { iconName: 'search', title: 'Optimisation SEO', description: 'Améliorez votre positionnement sur les moteurs de recherche avec des stratégies SEO data-driven et mesurables.' },
    { iconName: 'globe', title: 'Développement Web', description: 'Créez des sites web performants, responsives et optimisés pour la conversion avec les dernières technologies.' },
    { iconName: 'megaphone', title: 'Marketing Digital', description: 'Lancez des campagnes publicitaires ciblées sur Google Ads, Meta et d\'autres plateformes pour maximiser votre ROI.' },
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
    { iconName: 'mapPin', category: 'Référencement Local', subtitle: 'Sécurisations et Accès Google My Business', description: 'Protégez et sécurisez votre profil Google My Business avec un accès propriétaire garanti.', price: '353,89 €' },
    { iconName: 'star', category: 'Référencement Premium', subtitle: 'Référencement Optimale VIP+', description: 'Service premium exclusif avec garantie de résultats exceptionnels.', price: '750,79 €' },
    { iconName: 'messageSquare', category: 'Réputation en Ligne', subtitle: 'Gestion des Avis', description: 'Améliorez votre réputation en ligne avec notre service de gestion des avis.', price: '747,43 €' },
    { iconName: 'store', category: 'Marketing Local', subtitle: 'Présence Locales', description: 'Boostez votre visibilité locale avec une présence optimisée.', price: '2 200,00 €' },
    { iconName: 'globe', category: 'Développement Web', subtitle: 'Création Site Web', description: 'Création de site web professionnel avec protocole SSL, référencement optimisé.', price: '550,00 €' },
    { iconName: 'rocket', category: 'Google Premium', subtitle: 'Mise à jour 2026', description: 'Solution complète intégrant tous nos services premium.', price: '1 000,00 €' },
  ];

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'LMP Digital Services — Marketing Digital & Référencement Local',
      description: 'Propulsez votre visibilité au sommet. Expertise en marketing digital, référencement SEO, Google My Business et création de sites web. 500+ clients satisfaits.',
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
          setTimeout(() => this.reobserveAnimations(), 50);
        }
      },
    });
  }

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

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;
    this.setupScrollObserver();
  }

  private setupScrollObserver(): void {
    this.scrollObserver?.disconnect();
    this.scrollObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('animate-visible');
          } else {
            entry.target.classList.remove('animate-visible');
          }
        });
      },
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' },
    );

    document.querySelectorAll('.scroll-animate').forEach((el) => {
      this.scrollObserver!.observe(el);
    });
  }

  private reobserveAnimations(): void {
    document.querySelectorAll('.scroll-animate:not(.animate-visible)').forEach((el) => {
      this.scrollObserver?.observe(el);
    });
  }

  ngOnDestroy(): void {
    this.scrollObserver?.disconnect();
  }
}
