import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-parametres',
   standalone: true,
  imports: [FormsModule],
  templateUrl: './parametres.component.html',
  styleUrls: ['./parametres.component.css']
})
export class ParametresComponent {

  settings = {
    notifications: true,
    darkMode: false,
    language: 'fr',
    autoStartTour: false
  };

  save() {
    alert('Paramètres sauvegardés ✅');
  }

}
