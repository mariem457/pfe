import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

type FaqItem = {
  q: string;
  a: string;
  open: boolean;
};

@Component({
  selector: 'app-support',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support.component.html',
  styleUrls: ['./support.component.css']
})
export class SupportComponent {

  faq: FaqItem[] = [
    {
      q: "Je ne vois pas ma tournée",
      a: "Vérifiez votre connexion internet. Si le problème persiste, redémarrez l'application et réessayez.",
      open: true
    },
    {
      q: "La carte ne s'affiche pas",
      a: "Assurez-vous que l'autorisation de localisation est activée. Sinon, la carte peut rester vide.",
      open: false
    },
    {
      q: "Je ne peux pas 'Collecter' une poubelle",
      a: "Vous devez d'abord démarrer la tournée. Ensuite, réessayez. Si ça bloque, contactez le support.",
      open: false
    }
  ];

  form = {
    subject: '',
    message: '',
    urgency: 'normal' as 'low' | 'normal' | 'high',
  };

  toggle(item: FaqItem) {
    item.open = !item.open;
  }

  send() {
    if (!this.form.subject.trim() || !this.form.message.trim()) {
      alert('Veuillez remplir le sujet et le message.');
      return;
    }

    // TODO: بعدين تربطها بالbackend (Spring Boot)
    alert('Message envoyé au support ✅');
    this.form.subject = '';
    this.form.message = '';
    this.form.urgency = 'normal';
  }
}