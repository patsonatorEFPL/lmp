import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule, ArrowLeft, Loader2, Send, MessageSquare,
  Clock, CheckCircle, XCircle, AlertTriangle, User, Headset,
} from 'lucide-angular';
import { HlmButton } from '@spartan-ng/helm/button';
import { PortalStubService } from '../../core/stubs/portal-stub.service';
import { Issue, IssueStatus, IssuePriority } from '../../core/stubs/portal.models';

type Tone = 'neutral' | 'info' | 'warning' | 'success' | 'danger';

@Component({
  selector: 'lmp-user-ticket-detail',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, RouterLink, LucideAngularModule, HlmButton],
  template: `
    @if (loading()) {
      <div class="flex items-center justify-center py-16">
        <lucide-icon [img]="Loader2Icon" [size]="32" class="animate-spin text-(--primary)"></lucide-icon>
      </div>
    } @else if (!issue()) {
      <div class="flex flex-col items-center justify-center rounded-md border border-(--border) bg-(--card) py-16 text-center">
        <p class="text-sm font-medium text-(--foreground)">Ticket introuvable</p>
        <a routerLink="/dashboard/tickets" class="mt-3 text-sm text-(--primary) hover:underline">Retour à la liste</a>
      </div>
    } @else {
      <!-- Header -->
      <div class="flex items-center gap-2 pb-4">
        <a routerLink="/dashboard/tickets" hlmBtn variant="ghost" size="icon"
          class="h-8 w-8 cursor-pointer text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-900">
          <lucide-icon [img]="ArrowLeftIcon" [size]="16"></lucide-icon>
        </a>
        <div class="min-w-0">
          <p class="font-mono text-[10px] uppercase tracking-wider text-(--muted-foreground)">{{ issue()!.id }}</p>
          <h2 class="truncate text-base font-semibold text-(--foreground)">{{ issue()!.subject }}</h2>
        </div>
      </div>

      <!-- Metadata -->
      <div class="grid grid-cols-2 gap-3 md:grid-cols-4">
        <div class="rounded-md border border-(--border) bg-(--card) p-3">
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Statut</p>
          <span class="mt-1 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
            [ngClass]="toneClasses(statusTone(issue()!.status))">
            <lucide-icon [img]="statusIcon(issue()!.status)" [size]="12"></lucide-icon>
            {{ issue()!.status }}
          </span>
        </div>
        <div class="rounded-md border border-(--border) bg-(--card) p-3">
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Priorité</p>
          <p class="mt-1 inline-flex items-center gap-1 text-sm font-medium" [ngClass]="priorityClasses(issue()!.priority)">
            <lucide-icon [img]="AlertTriangleIcon" [size]="12"></lucide-icon>
            {{ issue()!.priority }}
          </p>
        </div>
        <div class="rounded-md border border-(--border) bg-(--card) p-3">
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Type</p>
          <p class="mt-1 text-sm text-(--foreground)">{{ issue()!.issueType }}</p>
        </div>
        <div class="rounded-md border border-(--border) bg-(--card) p-3">
          <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Ouvert le</p>
          <p class="mt-1 text-sm text-(--foreground)">{{ issue()!.openingDate | date: 'mediumDate' }}</p>
        </div>
      </div>

      <!-- Description -->
      <div class="mt-4 rounded-md border border-(--border) bg-(--card) p-4">
        <p class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Description</p>
        <p class="mt-2 whitespace-pre-line text-sm text-(--foreground)">{{ issue()!.description }}</p>
      </div>

      <!-- Thread -->
      <div class="mt-4">
        <div class="mb-3 flex items-center gap-2">
          <lucide-icon [img]="MessageSquareIcon" [size]="16" class="text-(--muted-foreground)"></lucide-icon>
          <h3 class="text-sm font-semibold text-(--foreground)">Conversation ({{ issue()!.comments.length }})</h3>
        </div>

        @if (issue()!.comments.length === 0) {
          <p class="rounded-md border border-(--border) bg-(--card) px-4 py-6 text-center text-sm text-(--muted-foreground)">
            Aucune réponse pour l'instant.
          </p>
        } @else {
          <ol class="space-y-3">
            @for (c of issue()!.comments; track c.id) {
              <li class="flex gap-3 rounded-md border border-(--border) bg-(--card) p-4"
                [ngClass]="c.authorRole === 'agent' ? 'border-l-4 border-l-(--primary)/60' : ''">
                <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-(--muted)">
                  <lucide-icon [img]="c.authorRole === 'agent' ? HeadsetIcon : UserIcon" [size]="14"
                    [ngClass]="c.authorRole === 'agent' ? 'text-(--primary)' : 'text-(--muted-foreground)'"></lucide-icon>
                </div>
                <div class="min-w-0 flex-1">
                  <div class="flex items-center justify-between gap-2">
                    <p class="text-sm font-medium text-(--foreground)">
                      {{ c.author }}
                      <span class="ml-1 text-[10px] uppercase tracking-wider text-(--muted-foreground)">
                        {{ c.authorRole === 'agent' ? 'Équipe LMP' : 'Vous' }}
                      </span>
                    </p>
                    <p class="shrink-0 text-xs text-(--muted-foreground)">{{ c.createdAt | date: 'short' }}</p>
                  </div>
                  <p class="mt-1.5 whitespace-pre-line text-sm text-(--foreground)">{{ c.message }}</p>
                </div>
              </li>
            }
          </ol>
        }
      </div>

      <!-- Reply form -->
      @if (issue()!.status !== 'Closed') {
        <form (submit)="sendReply($event)" class="mt-4 rounded-md border border-(--border) bg-(--card) p-4">
          <label for="reply" class="text-[11px] uppercase tracking-wider text-(--muted-foreground)">Votre réponse</label>
          <textarea id="reply" [(ngModel)]="replyMessage" name="replyMessage" rows="4"
            class="mt-2 w-full resize-y rounded-md border border-(--border) bg-(--background) p-2 text-sm text-(--foreground) outline-none focus:border-(--primary)/60"
            placeholder="Écrire un message…" required></textarea>
          <div class="mt-3 flex justify-end">
            <button hlmBtn size="sm" type="submit" class="cursor-pointer gap-1.5"
              [disabled]="!replyMessage.trim() || sending()">
              @if (sending()) {
                <lucide-icon [img]="Loader2Icon" [size]="14" class="animate-spin"></lucide-icon>
              } @else {
                <lucide-icon [img]="SendIcon" [size]="14"></lucide-icon>
              }
              Envoyer
            </button>
          </div>
        </form>
      } @else {
        <p class="mt-4 rounded-md border border-(--border) bg-(--muted)/40 px-4 py-3 text-center text-xs text-(--muted-foreground)">
          Ce ticket est clos. Ouvrez-en un nouveau pour toute question supplémentaire.
        </p>
      }
    }
  `,
})
export class UserTicketDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly stub = inject(PortalStubService);

  readonly ArrowLeftIcon = ArrowLeft;
  readonly Loader2Icon = Loader2;
  readonly SendIcon = Send;
  readonly MessageSquareIcon = MessageSquare;
  readonly AlertTriangleIcon = AlertTriangle;
  readonly UserIcon = User;
  readonly HeadsetIcon = Headset;

  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly issue = signal<Issue | null>(null);
  replyMessage = '';

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(id);
  }

  private load(id: string) {
    this.loading.set(true);
    this.stub.getIssue(id).subscribe({
      next: (i) => { this.issue.set(i ?? null); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  sendReply(e: Event) {
    e.preventDefault();
    const issue = this.issue();
    if (!issue || !this.replyMessage.trim()) return;
    this.sending.set(true);
    this.stub.addIssueComment(issue.id, this.replyMessage.trim()).subscribe({
      next: () => {
        this.replyMessage = '';
        this.sending.set(false);
        this.load(issue.id);
      },
      error: () => this.sending.set(false),
    });
  }

  statusTone(s: IssueStatus): Tone {
    switch (s) {
      case 'Open': return 'info';
      case 'Replied': return 'warning';
      case 'On Hold': return 'neutral';
      case 'Resolved': return 'success';
      case 'Closed': return 'neutral';
    }
  }

  statusIcon(s: IssueStatus) {
    switch (s) {
      case 'Resolved': return CheckCircle;
      case 'Closed': return XCircle;
      default: return Clock;
    }
  }

  priorityClasses(p: IssuePriority): string {
    switch (p) {
      case 'Urgent': return 'text-red-600 dark:text-red-400';
      case 'High': return 'text-orange-600 dark:text-orange-400';
      case 'Medium': return 'text-amber-600 dark:text-amber-400';
      case 'Low': return 'text-(--muted-foreground)';
    }
  }

  toneClasses(t: Tone): string {
    const map: Record<Tone, string> = {
      neutral: 'bg-zinc-100 text-zinc-700 dark:bg-zinc-900 dark:text-zinc-300',
      info: 'bg-blue-100 text-blue-700 dark:bg-blue-950/40 dark:text-blue-300',
      warning: 'bg-amber-100 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
      success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300',
      danger: 'bg-red-100 text-red-700 dark:bg-red-950/40 dark:text-red-300',
    };
    return map[t];
  }
}
