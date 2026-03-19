import { Component, inject, signal, AfterViewInit, OnDestroy, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  Mail,
  Phone,
  MapPin,
  Clock,
  Send,
  ChevronDown,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { SeoService } from '../../core/services/seo.service';

@Component({
  selector: 'lmp-contact',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton, HlmInput, HlmLabel],
  template: `
    <section class="relative">
      <div class="relative mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:px-8">
        <!-- Header -->
        <div class="mx-auto max-w-2xl text-center scroll-animate anim-fade-up">
          <h1 class="font-display text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
            Contact Marketing Digital
            <span class="hero-gradient-text block sm:inline">Professionnel</span>
          </h1>
          <p class="mt-4 text-lg text-(--muted-foreground)">
            Prêt à transformer votre présence digitale ? Notre équipe d'experts est là pour vous
            accompagner dans votre croissance en ligne. Contactez-nous pour une consultation gratuite.
          </p>
        </div>

        <!-- Content: Info + Form -->
        <div class="mt-14 grid grid-cols-1 gap-10 lg:grid-cols-5">
          <!-- Left: Contact Info -->
          <div class="flex flex-col gap-6 lg:col-span-2 scroll-animate anim-fade-left">
            <h2 class="font-display text-xl font-bold text-(--foreground)">
              Consultation SEO Gratuite
            </h2>
            <p class="text-sm text-(--muted-foreground) leading-relaxed">
              Que vous soyez une petite entreprise locale ou une grande organisation,
              nous avons les solutions pour améliorer votre visibilité digitale.
            </p>

            <!-- Contact Email -->
            <div class="flex items-start gap-4 scroll-animate anim-fade-up delay-100">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary)">
                <lucide-icon [img]="MailIcon" [size]="18"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">Contact Email</p>
                <a href="mailto:lmp.assistance@gmail.com" class="text-sm text-(--muted-foreground) hover:text-(--foreground) transition-colors">
                  lmp.assistance&#64;gmail.com
                </a>
                <p class="text-xs text-(--muted-foreground) mt-0.5">Réponse sous 24h</p>
              </div>
            </div>

            <!-- Bureau Marketing -->
            <div class="flex items-start gap-4 scroll-animate anim-fade-up delay-200">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary)">
                <lucide-icon [img]="MapPinIcon" [size]="18"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">Bureau Marketing</p>
                <p class="text-sm text-(--muted-foreground)">Rue Gatti De Gamond 97</p>
                <p class="text-sm text-(--muted-foreground)">1180 Uccle</p>
              </div>
            </div>

            <!-- Horaires -->
            <div class="flex items-start gap-4 scroll-animate anim-fade-up delay-300">
              <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-(--primary)/10 text-(--primary)">
                <lucide-icon [img]="PhoneIcon" [size]="18"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-semibold text-(--foreground)">Horaires</p>
                <p class="text-sm text-(--muted-foreground)">Lundi - Vendredi: 9h00 - 18h00</p>
                <p class="text-sm text-(--muted-foreground)">Samedi: 10h00 - 16h00</p>
                <p class="text-xs text-(--muted-foreground)">Dimanche: Fermé</p>
              </div>
            </div>

            <!-- Separator -->
            <div class="border-t border-(--border) pt-4 scroll-animate anim-fade-up delay-400">
              <h3 class="text-sm font-semibold text-(--foreground) mb-3">Suivez-nous</h3>
              <div class="flex items-center gap-3">
                <!-- TODO: Remplacer les href par les vrais comptes réseaux sociaux quand créés -->
                <a href="#" aria-label="LinkedIn — LMP Digital Services"
                  class="flex h-10 w-10 items-center justify-center rounded-full border border-(--border) text-(--muted-foreground) transition-colors hover:border-(--primary)/30 hover:text-(--primary)">
                  <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
                  </svg>
                </a>
                <a href="#" aria-label="Twitter / X — LMP Digital Services"
                  class="flex h-10 w-10 items-center justify-center rounded-full border border-(--border) text-(--muted-foreground) transition-colors hover:border-(--primary)/30 hover:text-(--primary)">
                  <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M22.46 6c-.77.35-1.6.58-2.46.69.88-.53 1.56-1.37 1.88-2.38-.83.5-1.75.85-2.72 1.05C18.37 4.5 17.26 4 16 4c-2.35 0-4.27 1.92-4.27 4.29 0 .34.04.67.11.98C8.28 9.09 5.11 7.38 3 4.79c-.37.63-.58 1.37-.58 2.15 0 1.49.75 2.81 1.91 3.56-.71 0-1.37-.2-1.95-.5v.03c0 2.08 1.48 3.82 3.44 4.21a4.22 4.22 0 0 1-1.93.07 4.28 4.28 0 0 0 4 2.98 8.521 8.521 0 0 1-5.33 1.84c-.34 0-.68-.02-1.02-.06C3.44 20.29 5.7 21 8.12 21 16 21 20.33 14.46 20.33 8.79c0-.19 0-.37-.01-.56.84-.6 1.56-1.36 2.14-2.23z"/>
                  </svg>
                </a>
                <a href="#" aria-label="Facebook — LMP Digital Services"
                  class="flex h-10 w-10 items-center justify-center rounded-full border border-(--border) text-(--muted-foreground) transition-colors hover:border-(--primary)/30 hover:text-(--primary)">
                  <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                  </svg>
                </a>
                <a href="#" aria-label="Instagram — LMP Digital Services"
                  class="flex h-10 w-10 items-center justify-center rounded-full border border-(--border) text-(--muted-foreground) transition-colors hover:border-(--primary)/30 hover:text-(--primary)">
                  <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zM12 0C8.741 0 8.333.014 7.053.072 2.695.272.273 2.69.073 7.052.014 8.333 0 8.741 0 12c0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98C8.333 23.986 8.741 24 12 24c3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98C15.668.014 15.259 0 12 0zm0 5.838a6.162 6.162 0 1 0 0 12.324 6.162 6.162 0 0 0 0-12.324zM12 16a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm6.406-11.845a1.44 1.44 0 1 0 0 2.881 1.44 1.44 0 0 0 0-2.881z"/>
                  </svg>
                </a>
              </div>
            </div>
          </div>

          <!-- Right: Form -->
          <div class="rounded-xl border border-(--border) bg-(--card) p-6 sm:p-8 lg:col-span-3 scroll-animate anim-fade-right">
            <h2 class="font-display text-lg font-bold text-(--foreground) mb-6">
              Formulaire Contact SEO
            </h2>
            <form (ngSubmit)="onSubmit()" class="space-y-5">
              <div class="space-y-2">
                <label hlmLabel>Nom complet *</label>
                <input
                  hlmInput
                  type="text"
                  placeholder="Jean Dupont"
                  [(ngModel)]="form.name"
                  name="name"
                  required
                />
              </div>

              <div class="space-y-2">
                <label hlmLabel>Email *</label>
                <input
                  hlmInput
                  type="email"
                  placeholder="jean&#64;entreprise.be"
                  [(ngModel)]="form.email"
                  name="email"
                  required
                />
              </div>

              <div class="space-y-2">
                <label hlmLabel>Sujet *</label>
                <input
                  hlmInput
                  type="text"
                  placeholder="Comment pouvons-nous vous aider ?"
                  [(ngModel)]="form.subject"
                  name="subject"
                  required
                />
              </div>

              <div class="grid grid-cols-1 gap-5 sm:grid-cols-2">
                <div class="space-y-2">
                  <label hlmLabel>Téléphone</label>
                  <input
                    hlmInput
                    type="tel"
                    placeholder="+32 2 123 45 67"
                    [(ngModel)]="form.phone"
                    name="phone"
                  />
                </div>
                <div class="space-y-2">
                  <label hlmLabel>Entreprise</label>
                  <input
                    hlmInput
                    type="text"
                    placeholder="Nom de votre entreprise"
                    [(ngModel)]="form.company"
                    name="company"
                  />
                </div>
              </div>

              <div class="space-y-2">
                <label hlmLabel>Message *</label>
                <textarea
                  hlmInput
                  rows="5"
                  placeholder="Décrivez votre projet et vos objectifs..."
                  [(ngModel)]="form.message"
                  name="message"
                  required
                  class="resize-none"
                ></textarea>
              </div>

              <!-- Consent checkbox -->
              <div class="flex items-start gap-2">
                <input
                  type="checkbox"
                  id="consent"
                  [(ngModel)]="form.consent"
                  name="consent"
                  class="mt-1 h-4 w-4 rounded border-gray-600 bg-transparent accent-(--primary) cursor-pointer"
                />
                <label for="consent" class="text-xs text-(--muted-foreground) cursor-pointer">
                  J'accepte que LMP me contacte concernant ma demande *
                </label>
              </div>

              @if (successMessage()) {
                <div class="rounded-lg bg-emerald-500/10 px-4 py-3 text-sm text-emerald-500">
                  {{ successMessage() }}
                </div>
              }

              @if (errorMessage()) {
                <div class="rounded-lg bg-(--destructive)/10 px-4 py-3 text-sm text-(--destructive)">
                  {{ errorMessage() }}
                </div>
              }

              <button
                hlmBtn
                variant="default"
                type="submit"
                class="w-full cursor-pointer gap-2"
                [disabled]="submitting()"
              >
                @if (submitting()) {
                  Envoi en cours...
                } @else {
                  <lucide-icon [img]="SendIcon" [size]="16"></lucide-icon>
                  Envoyer le message
                }
              </button>
            </form>
          </div>
        </div>

        <!-- FAQ Section -->
        <div class="mt-20 scroll-animate anim-blur-in">
          <div class="mx-auto max-w-3xl text-center mb-10">
            <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
              FAQ Services Marketing Digital
            </h2>
            <p class="mt-4 text-(--muted-foreground)">
              Trouvez rapidement les réponses aux questions les plus courantes sur nos services.
            </p>
          </div>

          <div class="mx-auto max-w-3xl space-y-3">
            @for (faq of faqs; track faq.question; let i = $index) {
              <div
                class="rounded-xl border border-(--border) bg-(--card) overflow-hidden scroll-animate anim-fade-up"
                [class]="'rounded-xl border border-(--border) bg-(--card) overflow-hidden scroll-animate anim-fade-up delay-' + (i + 1) + '00'"
              >
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

        <!-- CTA Section -->
        <section class="mt-20 rounded-2xl border border-(--border) bg-(--card) p-10 sm:p-16 text-center scroll-animate anim-slide-up">
          <h2 class="font-display text-3xl font-bold text-(--foreground) sm:text-4xl">
            Démarrer Votre Projet SEO
          </h2>
          <p class="mt-4 mx-auto max-w-xl text-(--muted-foreground)">
            Rejoignez des centaines d'entreprises qui ont déjà transformé leur présence en ligne avec LMP.
          </p>
          <div class="mt-8">
            <a
              href="mailto:lmp.assistance@gmail.com"
              class="inline-flex items-center gap-2 rounded-xl bg-(--primary) px-8 py-3.5 text-sm font-semibold text-white transition-colors hover:bg-blue-700 cursor-pointer"
            >
              <lucide-icon [img]="MailIcon" [size]="16"></lucide-icon>
              Nous contacter par email
            </a>
          </div>
        </section>
      </div>
    </section>
  `,
})
export class ContactComponent implements OnInit, AfterViewInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly seo = inject(SeoService);
  private isBrowser: boolean;
  private scrollObserver?: IntersectionObserver;

  constructor(@Inject(PLATFORM_ID) platformId: object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.seo.updateMeta({
      title: 'Contactez-nous — Consultation SEO Gratuite',
      description: 'Contactez LMP Digital Services pour une consultation SEO gratuite et sans engagement. Notre équipe d\'experts en marketing digital est là pour booster votre visibilité locale.',
      url: '/contact',
      keywords: 'contact LMP, consultation SEO gratuite, marketing digital contact, devis référencement, Uccle Bruxelles',
    });
  }

  readonly SendIcon = Send;
  readonly MailIcon = Mail;
  readonly MapPinIcon = MapPin;
  readonly PhoneIcon = Phone;
  readonly ClockIcon = Clock;
  readonly ChevronDownIcon = ChevronDown;

  readonly submitting = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly openFaqIndex = signal<number | null>(null);

  readonly form: {
    name: string;
    email: string;
    subject: string;
    phone: string;
    company: string;
    message: string;
    consent: boolean;
  } = {
    name: '',
    email: '',
    subject: '',
    phone: '',
    company: '',
    message: '',
    consent: false,
  };

  readonly faqs = [
    {
      question: 'Délais Résultats SEO',
      answer: 'Les premiers résultats de référencement sont généralement visibles entre 3 et 6 mois selon la compétitivité de votre secteur. Nous fournissons des rapports mensuels pour suivre l\'évolution de vos performances.',
    },
    {
      question: 'Consultation Gratuite Marketing',
      answer: 'Oui, nous proposons une consultation initiale gratuite et sans engagement. Lors de cet audit, nous analysons votre présence en ligne actuelle et identifions les opportunités d\'amélioration.',
    },
    {
      question: 'Quels types d\'entreprises accompagnez-vous ?',
      answer: 'Nous accompagnons tous types d\'entreprises : commerces locaux, artisans, professions libérales, PME et grandes entreprises. Notre expertise couvre tous les secteurs d\'activité.',
    },
    {
      question: 'Proposez-vous un suivi des performances ?',
      answer: 'Absolument. Chaque client bénéficie d\'un tableau de bord personnalisé avec des rapports détaillés sur les performances, le trafic, les conversions et le ROI de chaque action mise en place.',
    },
  ];

  toggleFaq(index: number): void {
    if (this.openFaqIndex() === index) {
      this.openFaqIndex.set(null);
    } else {
      this.openFaqIndex.set(index);
    }
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;

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
  }

  ngOnDestroy(): void {
    this.scrollObserver?.disconnect();
  }

  onSubmit(): void {
    if (!this.form.name || !this.form.email || !this.form.subject || !this.form.message) {
      this.errorMessage.set('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    if (!this.form.consent) {
      this.errorMessage.set('Veuillez accepter les conditions de contact.');
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.http
      .post(`${environment.apiUrl}/api/v1/contact`, this.form, { withCredentials: true })
      .subscribe({
        next: () => {
          this.successMessage.set('Message envoyé avec succès ! Nous vous répondrons dans les 24h.');
          this.form.name = '';
          this.form.email = '';
          this.form.subject = '';
          this.form.phone = '';
          this.form.company = '';
          this.form.message = '';
          this.form.consent = false;
          this.submitting.set(false);
        },
        error: () => {
          this.errorMessage.set("Une erreur est survenue. Veuillez réessayer.");
          this.submitting.set(false);
        },
      });
  }
}
