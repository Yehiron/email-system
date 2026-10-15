import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-root', standalone: true, imports: [FormsModule],
  template: `
    <main>
      <div class="eyebrow">ANGULAR / SPRING BOOT / CLOUDFLARE</div>
      <h1>Email<span>Lab</span><span class="dot">.</span></h1>
      <p class="intro">Tu primer envío empieza aquí.</p>
      <form #form="ngForm" (ngSubmit)="send()">
        <fieldset [disabled]="sending()">
          <legend>Acceso a la API</legend>
          <div class="credentials">
            <label>Usuario<input name="username" [(ngModel)]="username" required autocomplete="username"></label>
            <label>Contraseña<input name="password" type="password" [(ngModel)]="password" required autocomplete="current-password"></label>
          </div>
          <p class="hint">Las credenciales solo se mantienen en memoria durante esta sesión.</p>
          <label>Destinatario<input name="to" type="email" [(ngModel)]="to" required email maxlength="254" placeholder="persona@ejemplo.com"></label>
          <label>Asunto<input name="subject" [(ngModel)]="subject" required pattern=".*\\S.*" maxlength="150" placeholder="¿Qué quieres compartir?"></label>
          <label>Mensaje<textarea name="message" [(ngModel)]="message" required pattern="[\\s\\S]*\\S[\\s\\S]*" maxlength="5000" rows="7" placeholder="Escribe tu mensaje…"></textarea></label>
          <div class="form-footer"><small>Texto plano · {{ message.length }}/5000</small><button type="submit" [disabled]="form.invalid || sending()">{{ sending() ? 'Procesando…' : 'Enviar correo →' }}</button></div>
        </fieldset>
      </form>
      @if (feedback()) { <div role="status" aria-live="polite" class="feedback" [class.error]="error()">{{ feedback() }}</div> }
      <p class="hint">En la configuración inicial el envío es simulado. Un correo aceptado por el proveedor aún puede rebotar.</p>
    </main>
  `
})
export class AppComponent {
  private http = inject(HttpClient);
  username = 'alumno'; password = ''; to = ''; subject = ''; message = '';
  sending = signal(false); feedback = signal(''); error = signal(false);
  async send() {
    if (this.sending() || !this.to.trim() || !this.subject.trim() || !this.message.trim()) return;
    this.sending.set(true); this.feedback.set(''); this.error.set(false);
    try {
      const bytes = new TextEncoder().encode(`${this.username}:${this.password}`);
      const authorization = 'Basic ' + btoa(Array.from(bytes, b => String.fromCharCode(b)).join(''));
      const result = await firstValueFrom(this.http.post<{ status: string; message: string; requestId: string }>(
        '/api/v1/emails', { to: this.to.trim(), subject: this.subject.trim(), message: this.message },
        { headers: new HttpHeaders({ Authorization: authorization }) }));
      this.feedback.set(result.message + ' Referencia: ' + result.requestId);
    } catch (err) {
      this.error.set(true);
      this.feedback.set(err instanceof HttpErrorResponse ? err.error?.message ?? 'No se pudo contactar la API. Revisa su estado antes de reintentar.' : 'Error inesperado.');
    } finally { this.sending.set(false); }
  }
}
