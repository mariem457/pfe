import { Component } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-rapport-user',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './rapport-user.component.html',
  styleUrls: ['./rapport-user.component.css']
})
export class RapportUserComponent {
  // needed by template
  dragging: boolean = false;
  locating: boolean = false;

  photoFile?: File;
  photoPreview: string | null = null;

  address: string = '';
  description: string = '';

  coords: { lat: string; lng: string } | null = null;

  constructor(private location: Location) {}

  // header back button
  goBack(): void {
    this.location.back();
  }

  // input file select
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.setPhoto(file);

    input.value = ''; // reset input
  }

  // drag & drop handlers
  onDragOver(e: DragEvent): void {
    e.preventDefault();
    this.dragging = true;
  }

  onDragLeave(e: DragEvent): void {
    e.preventDefault();
    this.dragging = false;
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    this.dragging = false;

    const file = e.dataTransfer?.files?.[0];
    if (file) this.setPhoto(file);
  }

  removePhoto(): void {
    this.photoFile = undefined;
    this.photoPreview = null;
  }

  private setPhoto(file: File): void {
    // optional size check
    const max = 10 * 1024 * 1024;
    if (file.size > max) {
      alert('File too large (max 10MB).');
      return;
    }

    this.photoFile = file;

    const reader = new FileReader();
    reader.onload = () => {
      this.photoPreview = String(reader.result);
    };
    reader.readAsDataURL(file);
  }

  // geolocation
  detectLocation(): void {
    if (!navigator.geolocation) {
      alert('Geolocation not supported.');
      return;
    }

    this.locating = true;

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.locating = false;
        this.coords = {
          lat: pos.coords.latitude.toFixed(5),
          lng: pos.coords.longitude.toFixed(5)
        };
      },
      () => {
        this.locating = false;
        alert('Unable to get location. Please allow permission.');
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  submitReport(): void {
    const payload = {
      address: this.address,
      description: this.description,
      coords: this.coords,
      photoName: this.photoFile?.name ?? null
    };

    console.log('REPORT payload:', payload);
    alert('Report submitted (demo).');
  }
}
