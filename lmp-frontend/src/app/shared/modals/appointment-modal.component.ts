import { Component, EventEmitter, Input, Output, signal, computed, OnChanges, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { HlmButton } from '@spartan-ng/helm/button';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'lmp-appointment-modal',
  standalone: true,
  imports: [FormsModule, NgClass, HlmButton],
  styles: [`
    @keyframes modal-backdrop-in {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    @keyframes modal-slide-in {
      from { opacity: 0; transform: translateY(24px) scale(0.96); }
      to { opacity: 1; transform: translateY(0) scale(1); }
    }
    @keyframes modal-backdrop-out {
      from { opacity: 1; }
      to { opacity: 0; }
    }
    @keyframes modal-slide-out {
      from { opacity: 1; transform: translateY(0) scale(1); }
      to { opacity: 0; transform: translateY(16px) scale(0.97); }
    }
    @keyframes slot-pop {
      0% { transform: scale(0.8); opacity: 0; }
      60% { transform: scale(1.05); }
      100% { transform: scale(1); opacity: 1; }
    }
    @keyframes pulse-ring {
      0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.4); }
      70% { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
      100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
    }
    @keyframes shake-error {
      0%, 100% { transform: translateX(0); }
      20%       { transform: translateX(-6px); }
      40%       { transform: translateX(6px); }
      60%       { transform: translateX(-4px); }
      80%       { transform: translateX(4px); }
    }
    .modal-backdrop-enter { animation: modal-backdrop-in 0.25s ease-out forwards; }
    .modal-slide-enter    { animation: modal-slide-in 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
    .modal-backdrop-exit  { animation: modal-backdrop-out 0.2s ease-in forwards; }
    .modal-slide-exit     { animation: modal-slide-out 0.2s ease-in forwards; }
    .slot-animate         { animation: slot-pop 0.25s ease-out forwards; }
    .slot-selected-ring   { animation: pulse-ring 1.5s ease-out infinite; }
    .field-error          { animation: shake-error 0.35s ease-out; }
  `],
  template: `
    @if (visible()) {
      <!-- Backdrop -->
      <div
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
        [ngClass]="closing() ? 'modal-backdrop-exit' : 'modal-backdrop-enter'"
        (click)="animateClose()"
      >
        <!-- Modal -->
        <div
          class="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-sm border border-(--border) bg-(--card) p-6 sm:p-8 shadow-sm"
          [ngClass]="closing() ? 'modal-slide-exit' : 'modal-slide-enter'"
          (click)="$event.stopPropagation()"
        >
          <!-- Close button -->
          <button
            class="absolute top-4 right-4 flex h-8 w-8 items-center justify-center rounded-full transition-all duration-200 text-(--muted-foreground) hover:text-(--foreground) hover:bg-(--accent) cursor-pointer"
            (click)="animateClose()"
          >
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>

          <!-- Header -->
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-sm bg-(--primary) text-(--primary-foreground)">
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <div>
              <h2 class="text-xl font-bold text-(--foreground)">Prendre rendez-vous</h2>
              <p class="text-sm text-(--muted-foreground)">Sélectionnez une date et un créneau horaire</p>
            </div>
          </div>

          <!-- Global error banner -->
          @if (submitted() && hasErrors()) {
            <div class="mt-4 flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/40 dark:bg-red-900/20 dark:text-red-400 field-error">
              <svg class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
              </svg>
              <span>Veuillez remplir tous les champs obligatoires et sélectionner un créneau.</span>
            </div>
          }

          <div class="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
            <!-- Step 1: Info -->
            <div>
              <div class="flex items-center gap-2 mb-4">
                <span class="flex h-6 w-6 items-center justify-center rounded-full bg-(--primary) text-xs font-bold text-(--primary-foreground)">1</span>
                <h3 class="text-sm font-semibold text-(--foreground)">Vos informations</h3>
              </div>

              <div class="space-y-3">
                <!-- Nom -->
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Nom complet *</label>
                  <input
                    type="text"
                    [(ngModel)]="fullName"
                    (ngModelChange)="clearError('fullName')"
                    class="mt-1 w-full rounded-lg border px-3 py-2.5 text-sm text-(--foreground) bg-(--background) outline-none transition-colors"
                    [class]="fieldError('fullName')
                      ? 'border-red-400 focus:border-red-500'
                      : 'border-(--border) focus:border-blue-500/50'"
                    placeholder="Jean Dupont"
                  />
                  @if (fieldError('fullName')) {
                    <p class="mt-1 text-xs text-red-500">Le nom complet est requis.</p>
                  }
                </div>

                <!-- Email -->
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Email *</label>
                  <input
                    type="email"
                    [(ngModel)]="email"
                    (ngModelChange)="clearError('email')"
                    class="mt-1 w-full rounded-lg border px-3 py-2.5 text-sm text-(--foreground) bg-(--background) outline-none transition-colors"
                    [class]="fieldError('email')
                      ? 'border-red-400 focus:border-red-500'
                      : 'border-(--border) focus:border-blue-500/50'"
                    placeholder="jean@exemple.com"
                  />
                  @if (fieldError('email')) {
                    <p class="mt-1 text-xs text-red-500">
                      {{ emailErrorMsg() }}
                    </p>
                  }
                </div>

                <!-- Téléphone -->
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Téléphone *</label>
                  <input
                    type="tel"
                    [(ngModel)]="phone"
                    (ngModelChange)="clearError('phone')"
                    class="mt-1 w-full rounded-lg border px-3 py-2.5 text-sm text-(--foreground) bg-(--background) outline-none transition-colors"
                    [class]="fieldError('phone')
                      ? 'border-red-400 focus:border-red-500'
                      : 'border-(--border) focus:border-blue-500/50'"
                    placeholder="+33 6 00 00 00 00"
                  />
                  @if (fieldError('phone')) {
                    <p class="mt-1 text-xs text-red-500">Le numéro de téléphone est requis.</p>
                  }
                </div>

                <!-- Service -->
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Service *</label>
                  <select
                    [(ngModel)]="selectedService"
                    (ngModelChange)="clearError('service')"
                    class="mt-1 w-full rounded-lg border px-3 py-2.5 text-sm text-(--foreground) bg-(--background) outline-none cursor-pointer transition-colors"
                    [class]="fieldError('service')
                      ? 'border-red-400 focus:border-red-500'
                      : 'border-(--border) focus:border-blue-500/50'"
                  >
                    <option value="">Sélectionnez un service...</option>
                    <option value="consultation">Consultation générale</option>
                    <option value="web">Développement Web</option>
                    <option value="seo">Référencement SEO</option>
                    <option value="formation">Formation</option>
                    <option value="audit">Audit technique</option>
                    <option value="marketing">Marketing Digital</option>
                    <option value="security">Sécurité Web</option>
                    <option value="other">Autre</option>
                  </select>
                  @if (fieldError('service')) {
                    <p class="mt-1 text-xs text-red-500">Veuillez sélectionner un service.</p>
                  }
                </div>

                <!-- Message -->
                <div>
                  <label class="text-xs font-medium text-(--foreground)">Message (optionnel)</label>
                  <textarea
                    [(ngModel)]="message"
                    rows="3"
                    class="mt-1 w-full rounded-lg border border-(--border) bg-(--background) px-3 py-2.5 text-sm text-(--foreground) outline-none resize-none focus:border-blue-500/50"
                  ></textarea>
                </div>
              </div>
            </div>

            <!-- Step 2: Calendar -->
            <div>
              <div class="flex items-center gap-2 mb-4">
                <span class="flex h-6 w-6 items-center justify-center rounded-full bg-(--primary) text-xs font-bold text-(--primary-foreground)">2</span>
                <h3 class="text-sm font-semibold text-(--foreground)">Disponibilités</h3>
              </div>

              <!-- Month navigation -->
              <div class="flex items-center justify-between mb-3">
                <button class="text-(--muted-foreground) hover:text-(--foreground) cursor-pointer" (click)="prevMonth()">
                  <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
                <h4 class="text-sm font-semibold text-(--foreground)">
                  {{ monthNames[currentMonth()] }} {{ currentYear() }}
                </h4>
                <button class="text-(--muted-foreground) hover:text-(--foreground) cursor-pointer" (click)="nextMonth()">
                  <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>

              <!-- Day headers -->
              <div class="grid grid-cols-7 text-center text-xs font-medium text-(--muted-foreground) mb-1">
                @for (day of dayNames; track day) {
                  <div class="py-1">{{ day }}</div>
                }
              </div>

              <!-- Calendar days -->
              <div class="grid grid-cols-7 text-center text-sm gap-0.5">
                @for (day of calendarDays(); track $index) {
                  @if (day === 0) {
                    <div></div>
                  } @else {
                    <button
                      class="rounded-lg py-1.5 transition-colors cursor-pointer"
                      [class]="
                        selectedDay() === day
                          ? 'bg-(--primary) text-(--primary-foreground) font-semibold'
                          : isPastDay(day)
                            ? 'text-(--muted-foreground)/40 cursor-not-allowed'
                            : 'text-(--foreground) hover:bg-blue-500/10'
                      "
                      [disabled]="isPastDay(day)"
                      (click)="selectDay(day)"
                    >
                      {{ day }}
                    </button>
                  }
                }
              </div>

              <!-- Time slots -->
              <div class="mt-4">
                <div class="flex items-center justify-between mb-2">
                  <h5 class="text-xs font-semibold text-(--foreground)">Créneaux disponibles</h5>
                  @if (submitted() && !selectedDay()) {
                    <p class="text-xs text-red-500">Sélectionnez une date.</p>
                  } @else if (submitted() && selectedDay() && !selectedTime()) {
                    <p class="text-xs text-red-500">Sélectionnez un créneau.</p>
                  }
                </div>

                <!-- Slot error highlight border -->
                <div
                  [class]="submitted() && (!selectedDay() || !selectedTime())
                    ? 'rounded-sm border border-red-300 p-2 dark:border-red-700/50'
                    : ''"
                >
                  @if (selectedDay()) {
                    <!-- Morning -->
                    <p class="text-[10px] font-medium text-(--muted-foreground) uppercase tracking-wider mb-1.5">🌅 Matin</p>
                    <div class="grid grid-cols-3 gap-1.5 mb-3">
                      @for (slot of morningSlots; track slot; let i = $index) {
                        <button
                          class="slot-animate rounded-lg border px-2 py-2 text-xs font-medium transition-all duration-200 cursor-pointer [1.04] active:scale-95"
                          [style.animation-delay]="i * 40 + 'ms'"
                          [ngClass]="
                            selectedTime() === slot
                              ? 'border-(--primary) bg-(--primary)/10 text-(--primary) font-semibold slot-selected-ring'
                              : 'border-(--border) text-(--muted-foreground) hover:border-blue-500/40 hover:text-(--foreground) hover:bg-blue-500/5'
                          "
                          (click)="selectTime(slot)"
                        >
                          {{ slot }}
                        </button>
                      }
                    </div>
                    <!-- Afternoon -->
                    <p class="text-[10px] font-medium text-(--muted-foreground) uppercase tracking-wider mb-1.5">☀️ Après-midi</p>
                    <div class="grid grid-cols-3 gap-1.5">
                      @for (slot of afternoonSlots; track slot; let i = $index) {
                        <button
                          class="slot-animate rounded-lg border px-2 py-2 text-xs font-medium transition-all duration-200 cursor-pointer [1.04] active:scale-95"
                          [style.animation-delay]="(i + 6) * 40 + 'ms'"
                          [ngClass]="
                            selectedTime() === slot
                              ? 'border-(--primary) bg-(--primary)/10 text-(--primary) font-semibold slot-selected-ring'
                              : 'border-(--border) text-(--muted-foreground) hover:border-blue-500/40 hover:text-(--foreground) hover:bg-blue-500/5'
                          "
                          (click)="selectTime(slot)"
                        >
                          {{ slot }}
                        </button>
                      }
                    </div>
                  } @else {
                    <div class="flex flex-col items-center py-4 text-center">
                      <svg
                        class="h-8 w-8 mb-2"
                        [class]="submitted() ? 'text-red-400' : 'text-(--muted-foreground)/40'"
                        fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                      <p class="text-xs" [class]="submitted() ? 'text-red-500' : 'text-(--muted-foreground)'">
                        Sélectionnez une date pour voir les créneaux
                      </p>
                    </div>
                  }
                </div>
              </div>

              <!-- Selection summary -->
              @if (selectedDay() && selectedTime()) {
                <div class="mt-4 rounded-lg border border-(--primary)/20 bg-gradient-to-r from-(--primary)/5 to-transparent p-3 slot-animate">
                  <div class="flex items-center gap-2 text-xs">
                    <span class="flex h-5 w-5 items-center justify-center rounded-full bg-(--primary)/10 text-(--primary)">✓</span>
                    <span class="font-semibold text-(--foreground)">Votre sélection</span>
                  </div>
                  <p class="mt-1 text-xs text-(--muted-foreground)">
                    {{ selectedDay() }} {{ monthNames[currentMonth()] }} {{ currentYear() }} à {{ selectedTime() }}
                  </p>
                </div>
              }
            </div>
          </div>

          <!-- Success message -->
          @if (submitSuccess()) {
            <div class="mt-4 flex items-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:border-emerald-800/40 dark:bg-emerald-900/20 dark:text-emerald-400">
              <svg class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
              </svg>
              <span>Votre rendez-vous a été enregistré avec succès ! Nous vous contacterons sous peu.</span>
            </div>
          }

          <!-- API error message -->
          @if (submitError()) {
            <div class="mt-4 flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:border-red-800/40 dark:bg-red-900/20 dark:text-red-400">
              <svg class="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
              </svg>
              <span>{{ submitError() }}</span>
            </div>
          }

          <!-- Actions -->
          <div class="mt-6 flex gap-3">
            <button
              class="flex-1 rounded-lg border border-(--border) px-4 py-2.5 text-sm font-medium text-(--muted-foreground) transition-colors hover:text-(--foreground) cursor-pointer"
              (click)="close()"
            >
              {{ submitSuccess() ? 'Fermer' : 'Annuler' }}
            </button>
            @if (!submitSuccess()) {
              <button
                hlmBtn
                variant="default"
                class="flex-1 cursor-pointer justify-center"
                [disabled]="submitting()"
                (click)="submit()"
              >
                @if (submitting()) {
                  <svg class="mr-2 h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M4 12a8 8 0 018-8v4l3-3-3-3v4a8 8 0 100 16 8 8 0 010-16z" />
                  </svg>
                  Envoi en cours...
                } @else {
                  Confirmer le rendez-vous
                }
              </button>
            }
          </div>
        </div>
      </div>
    }
  `,
})
export class AppointmentModalComponent implements OnChanges {
  private readonly http = inject(HttpClient);

  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();

  readonly visible  = signal(false);
  readonly closing  = signal(false);
  readonly submitted = signal(false);
  readonly submitting = signal(false);
  readonly submitSuccess = signal(false);
  readonly submitError = signal<string | null>(null);

  /** Fields that have failed validation after a submit attempt */
  private readonly errors = signal<Set<string>>(new Set());

  fullName        = '';
  email           = '';
  phone           = '';
  selectedService = '';
  message         = '';

  readonly monthNames = [
    'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
    'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
  ];
  readonly dayNames       = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];
  readonly morningSlots   = ['09:00', '09:30', '10:00', '10:30', '11:00', '11:30'];
  readonly afternoonSlots = ['14:00', '14:30', '15:00', '15:30', '16:00', '16:30'];

  currentMonth = signal(new Date().getMonth());
  currentYear  = signal(new Date().getFullYear());
  selectedDay  = signal<number | null>(null);
  selectedTime = signal<string | null>(null);

  readonly hasErrors = computed(() => this.errors().size > 0 || !this.selectedDay() || !this.selectedTime());

  readonly emailErrorMsg = computed(() => {
    if (!this.email.trim()) return 'L\'adresse email est requise.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) return 'Adresse email invalide.';
    return '';
  });

  readonly calendarDays = computed(() => {
    const year     = this.currentYear();
    const month    = this.currentMonth();
    const firstDay = new Date(year, month, 1).getDay();
    const total    = new Date(year, month + 1, 0).getDate();
    const days: number[] = [];
    for (let i = 0; i < firstDay; i++) days.push(0);
    for (let d = 1; d <= total; d++) days.push(d);
    return days;
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']) {
      if (this.isOpen) {
        this.visible.set(true);
        this.closing.set(false);
      }
    }
  }

  /** Returns true when the field should show an error highlight */
  fieldError(field: string): boolean {
    return this.submitted() && this.errors().has(field);
  }

  /** Clear a single field error once the user starts correcting it */
  clearError(field: string): void {
    const s = new Set(this.errors());
    s.delete(field);
    this.errors.set(s);
  }

  selectDay(day: number): void {
    if (!this.isPastDay(day)) {
      this.selectedDay.set(day);
      this.selectedTime.set(null);
    }
  }

  selectTime(slot: string): void {
    this.selectedTime.set(slot);
  }

  isPastDay(day: number): boolean {
    const today     = new Date();
    const checkDate = new Date(this.currentYear(), this.currentMonth(), day);
    return checkDate < new Date(today.getFullYear(), today.getMonth(), today.getDate());
  }

  prevMonth(): void {
    if (this.currentMonth() === 0) { this.currentMonth.set(11); this.currentYear.update(y => y - 1); }
    else { this.currentMonth.update(m => m - 1); }
    this.selectedDay.set(null);
    this.selectedTime.set(null);
  }

  nextMonth(): void {
    if (this.currentMonth() === 11) { this.currentMonth.set(0); this.currentYear.update(y => y + 1); }
    else { this.currentMonth.update(m => m + 1); }
    this.selectedDay.set(null);
    this.selectedTime.set(null);
  }

  /** Validate all fields, mark errors and stop if invalid */
  submit(): void {
    this.submitted.set(true);

    const errs = new Set<string>();

    if (!this.fullName.trim())                                      errs.add('fullName');
    if (!this.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) errs.add('email');
    if (!this.phone.trim())                                         errs.add('phone');
    if (!this.selectedService)                                      errs.add('service');

    this.errors.set(errs);

    if (errs.size > 0 || !this.selectedDay() || !this.selectedTime()) {
      return; // stop — errors displayed in template
    }

    // ✅ All valid — call the API
    this.submitting.set(true);
    this.submitError.set(null);

    const year = this.currentYear();
    const month = String(this.currentMonth() + 1).padStart(2, '0');
    const day = String(this.selectedDay()!).padStart(2, '0');

    const payload = {
      name: this.fullName.trim(),
      email: this.email.trim(),
      phone: this.phone.trim(),
      service: this.selectedService,
      date: `${year}-${month}-${day}`,
      time: this.selectedTime(),
      message: this.message.trim() || null,
    };

    this.http
      .post<{ success: boolean; message?: string }>(
        `${environment.apiUrl}/api/v1/appointments`,
        payload,
        { withCredentials: true },
      )
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.submitSuccess.set(true);
          // Auto-close after 3 seconds
          setTimeout(() => this.animateClose(), 3000);
        },
        error: (err) => {
          this.submitting.set(false);
          const msg = err.error?.message || 'Une erreur est survenue. Veuillez réessayer.';
          this.submitError.set(msg);
        },
      });
  }

  animateClose(): void {
    this.closing.set(true);
    setTimeout(() => {
      this.visible.set(false);
      this.closing.set(false);
      this.closed.emit();
      this._reset();
    }, 200);
  }

  close(): void { this.animateClose(); }

  private _reset(): void {
    this.submitted.set(false);
    this.submitting.set(false);
    this.submitSuccess.set(false);
    this.submitError.set(null);
    this.errors.set(new Set());
    this.fullName        = '';
    this.email           = '';
    this.phone           = '';
    this.selectedService = '';
    this.message         = '';
    this.selectedDay.set(null);
    this.selectedTime.set(null);
  }
}
