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
  ArrowRight,
  Calendar,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmLabel } from '@spartan-ng/helm/label';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { SeoService } from '../../core/services/seo.service';
import { AppointmentModalComponent } from '../../shared/modals/appointment-modal.component';

@Component({
  selector: 'lmp-contact',
  standalone: true,
  imports: [FormsModule, LucideAngularModule, HlmButton, HlmInput, HlmLabel, AppointmentModalComponent],
  template: `
    <section class="relative">
      <div class="relative mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <!-- Header -->
        <div class="mx-auto max-w-2xl text-center">
          <h1 class="text-4xl font-bold tracking-tight text-(--foreground) sm:text-5xl">
            Parlons de votre projet
          </h1>
          <p class="mt-3 text-base text-(--muted-foreground)">
            Notre équipe d'experts est là pour vous accompagner dans votre croissance en ligne.
          </p>
        </div>

        <!-- Content: Info + Form -->
        <div class="mt-12 grid grid-cols-1 gap-8 lg:grid-cols-5">
          <!-- Left: Contact Info -->
          <div class="flex flex-col gap-5 lg:col-span-2 scroll-animate">
            <h2 class="text-lg font-semibold text-(--foreground)">
              Consultation Gratuite
            </h2>
            <p class="text-sm text-(--muted-foreground) leading-relaxed">
              Que vous soyez une petite entreprise locale ou une grande organisation,
              nous avons les solutions pour améliorer votre visibilité digitale.
            </p>

            <div class="flex items-start gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
                <lucide-icon [img]="MailIcon" [size]="16"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-medium text-(--foreground)">Email</p>
                <a href="mailto:lmp.assistance@gmail.com" class="text-sm text-(--muted-foreground) hover:text-(--foreground) transition-colors">
                  lmp.assistance&#64;gmail.com
                </a>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
                <lucide-icon [img]="MapPinIcon" [size]="16"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-medium text-(--foreground)">Bureau</p>
                <p class="text-sm text-(--muted-foreground)">1085 Rue de la Rivière, Québec, QC G1Y 2A3</p>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-sm bg-(--muted) text-(--primary)">
                <lucide-icon [img]="PhoneIcon" [size]="16"></lucide-icon>
              </div>
              <div>
                <p class="text-sm font-medium text-(--foreground)">Horaires</p>
                <p class="text-sm text-(--muted-foreground)">Lundi - Vendredi: 9h00 - 18h00</p>
              </div>
            </div>

            <button
              (click)="showAppointment.set(true)"
              class="mt-2 inline-flex items-center gap-2 rounded-sm bg-(--primary) px-5 py-2.5 text-sm font-medium text-(--primary-foreground) transition-colors hover:opacity-90 cursor-pointer w-fit"
            >
              <lucide-icon [img]="CalendarIcon" [size]="16"></lucide-icon>
              Réserver un créneau
            </button>
          </div>

          <!-- Right: Form -->
          <div class="rounded-sm border border-(--border) bg-(--card) p-6 lg:col-span-3 scroll-animate">
            <h2 class="text-base font-semibold text-(--foreground) mb-5">
              Formulaire de Contact
            </h2>
            <form (ngSubmit)="onSubmit()" class="space-y-4">
              <div class="space-y-1.5">
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

              <div class="space-y-1.5">
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

              <div class="space-y-1.5">
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

              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div class="space-y-1.5">
                  <label hlmLabel>Téléphone</label>
                  <input
                    hlmInput
                    type="tel"
                    placeholder="+32 2 123 45 67"
                    [(ngModel)]="form.phone"
                    name="phone"
                  />
                </div>
                <div class="space-y-1.5">
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

              <div class="space-y-1.5">
                <label hlmLabel>Message *</label>
                <textarea
                  hlmInput
                  rows="4"
                  placeholder="Décrivez votre projet et vos objectifs..."
                  [(ngModel)]="form.message"
                  name="message"
                  required
                  class="resize-none"
                ></textarea>
              </div>

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
                <div class="rounded-sm bg-(--success)/10 px-3 py-2 text-sm text-(--success)">
                  {{ successMessage() }}
                </div>
              }

              @if (errorMessage()) {
                <div class="rounded-sm bg-(--destructive)/10 px-3 py-2 text-sm text-(--destructive)">
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
        <div class="mt-16 scroll-animate">
          <div class="grid grid-cols-1 lg:grid-cols-5 gap-10">
            <div class="lg:col-span-2">
              <h2 class="text-2xl font-bold text-(--foreground)">
                Questions fréquentes
              </h2>
              <p class="mt-3 text-sm text-(--muted-foreground)">
                Vous ne trouvez pas votre réponse ?
                <a href="mailto:lmp.assistance@gmail.com" class="text-(--primary) hover:underline">Écrivez-nous</a>
              </p>
            </div>

            <div class="lg:col-span-3 space-y-2">
              @for (faq of faqs; track faq.question; let i = $index) {
                <div class="rounded-sm border border-(--border) bg-(--card) overflow-hidden">
                  <button
                    class="flex w-full items-center justify-between px-4 py-3 text-left cursor-pointer"
                    (click)="toggleFaq(i)"
                  >
                    <span class="text-sm font-medium text-(--foreground) pr-4">{{ faq.question }}</span>
                    <lucide-icon
                      [img]="ChevronDownIcon"
                      [size]="16"
                      class="text-(--muted-foreground) transition-transform duration-150 shrink-0"
                      [class.rotate-180]="openFaqIndex() === i"
                    ></lucide-icon>
                  </button>
                  @if (openFaqIndex() === i) {
                    <div class="px-4 pb-3 border-t border-(--border) pt-3">
                      <p class="text-sm leading-relaxed text-(--muted-foreground)">{{ faq.answer }}</p>
                    </div>
                  }
                </div>
              }
            </div>
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
      description: 'Contactez LMP Digital Services pour une consultation SEO gratuite et sans engagement.',
      url: '/contact',
      keywords: 'contact LMP, consultation SEO gratuite, marketing digital contact, devis référencement, Québec Canada',
    });
  }

  readonly SendIcon = Send;
  readonly MailIcon = Mail;
  readonly MapPinIcon = MapPin;
  readonly PhoneIcon = Phone;
  readonly ClockIcon = Clock;
  readonly ChevronDownIcon = ChevronDown;
  readonly ArrowRightIcon = ArrowRight;
  readonly CalendarIcon = Calendar;

  readonly submitting = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly openFaqIndex = signal<number | null>(null);
  readonly showAppointment = signal(false);

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
      question: 'Combien de temps faut-il pour voir des résultats SEO ?',
      answer: 'Les premiers résultats de référencement sont généralement visibles entre 3 et 6 mois selon la compétitivité de votre secteur.',
    },
    {
      question: 'La consultation initiale est-elle vraiment gratuite ?',
      answer: 'Oui, nous proposons une consultation initiale gratuite et sans engagement.',
    },
    {
      question: 'Quels types d\'entreprises accompagnez-vous ?',
      answer: 'Nous accompagnons tous types d\'entreprises : commerces locaux, artisans, professions libérales, PME et grandes entreprises.',
    },
    {
      question: 'Proposez-vous un suivi des performances ?',
      answer: 'Absolument. Chaque client bénéficie d\'un tableau de bord personnalisé avec des rapports détaillés.',
    },
  ];

  toggleFaq(index: number): void {
    this.openFaqIndex.set(this.openFaqIndex() === index ? null : index);
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) return;

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
