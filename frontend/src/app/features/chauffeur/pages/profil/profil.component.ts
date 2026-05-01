import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [FormsModule, NgIf],
  templateUrl: './profil.component.html',
  styleUrls: ['./profil.component.css']
})
export class ProfilComponent {
  chauffeur = {
    name: 'Chauffeur Name',
    email: 'chauffeur@city.gov',
    phone: '+216 00 000 000',
    cin: '12345678',
    truckId: 'TRUCK-01',
    zone: 'Zone Nord',
    status: 'En service'
  };

  editMode = false;

  toggleEdit() {
    this.editMode = !this.editMode;
  }

  save() {
    this.editMode = false;
    alert('Profil mis à jour ✅');
  }
}