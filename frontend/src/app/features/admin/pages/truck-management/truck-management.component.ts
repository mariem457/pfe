import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  FuelType,
  TruckRequest,
  TruckResponse,
  TruckService
} from '../../../../services/truck.service';

import {
  DriverResponse,
  DriverService
} from '../../../../services/driver.service';

@Component({
  selector: 'app-truck-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './truck-management.component.html',
  styleUrls: ['./truck-management.component.css']
})
export class TruckManagementComponent implements OnInit {
  trucks: TruckResponse[] = [];
  filteredTrucks: TruckResponse[] = [];
  availableDrivers: DriverResponse[] = [];

  loading = false;
  showForm = false;

  selectedTruckId: number | null = null;
  selectedTruck: TruckResponse | null = null;

  searchTerm = '';
  errorMessage = '';
  successMessage = '';

  fuelTypes: FuelType[] = ['DIESEL', 'ESSENCE', 'ELECTRIC', 'HYBRID'];

  form: Partial<TruckRequest> = this.emptyForm();

  constructor(
    private truckService: TruckService,
    private driverService: DriverService
  ) {}

  ngOnInit(): void {
    this.loadTrucks();
  }

  emptyForm(): Partial<TruckRequest> {
    return {
      assignedDriverId: null,
      maxLoadKg: 1000,
      tankCapacityLiters: 100,
      fuelConsumptionPerKm: 0.25,
      fuelType: 'DIESEL'
    };
  }

  loadTrucks(): void {
    this.loading = true;
    this.clearMessages();

    this.truckService.getAll().subscribe({
      next: (data) => {
        this.trucks = data || [];
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les camions.';
        this.loading = false;
      }
    });
  }

  loadAvailableDrivers(currentTruck?: TruckResponse): void {
    this.driverService.getAllDrivers().subscribe({
      next: (drivers) => {
        const assignedDriverIds = this.trucks
          .filter(t => t.id !== currentTruck?.id)
          .map(t => t.assignedDriverId)
          .filter((id): id is number => id !== null && id !== undefined);

        this.availableDrivers = (drivers || []).filter(driver =>
          !assignedDriverIds.includes(driver.id)
        );

        if (
          currentTruck?.assignedDriverId &&
          !this.availableDrivers.some(d => d.id === currentTruck.assignedDriverId)
        ) {
          this.availableDrivers.unshift({
            id: currentTruck.assignedDriverId,
            fullName: currentTruck.assignedDriverName || 'Chauffeur affecté'
          });
        }
      },
      error: () => {
        this.availableDrivers = [];
        this.errorMessage = 'Impossible de charger la liste des chauffeurs.';
      }
    });
  }

  openEditForm(truck: TruckResponse): void {
    this.showForm = true;
    this.selectedTruckId = truck.id;
    this.selectedTruck = truck;

    this.form = {
      truckCode: truck.truckCode,
      plateNumber: truck.plateNumber,
      brand: truck.brand,
      model: truck.model,
      status: truck.status,
      isActive: truck.isActive,

      assignedDriverId: truck.assignedDriverId || null,
      maxLoadKg: Number(truck.maxLoadKg || 0),
      tankCapacityLiters: Number(truck.tankCapacityLiters || 0),
      fuelConsumptionPerKm: Number(truck.fuelConsumptionPerKm || 0),
      fuelType: truck.fuelType || 'DIESEL',

      fuelLevelLiters: Number(truck.fuelLevelLiters || 0),
      currentLoadKg: Number(truck.currentLoadKg || 0),
      maxBinCapacity: truck.maxBinCapacity,
      lastKnownLat: truck.lastKnownLat,
      lastKnownLng: truck.lastKnownLng
    };

    this.loadAvailableDrivers(truck);
    this.clearMessages();
  }

  closeForm(): void {
    this.showForm = false;
    this.selectedTruckId = null;
    this.selectedTruck = null;
    this.form = this.emptyForm();
    this.availableDrivers = [];
  }

  saveTruck(): void {
    this.clearMessages();

    if (!this.selectedTruckId || !this.selectedTruck) {
      this.errorMessage = 'Aucun camion sélectionné.';
      return;
    }

    if (!this.form.fuelType) {
      this.errorMessage = 'Type carburant obligatoire.';
      return;
    }

    const payload: TruckRequest = {
      truckCode: this.selectedTruck.truckCode,
      plateNumber: this.selectedTruck.plateNumber,
      brand: this.selectedTruck.brand,
      model: this.selectedTruck.model,
      status: this.selectedTruck.status,
      isActive: this.selectedTruck.isActive,

      assignedDriverId: this.form.assignedDriverId || null,
      maxLoadKg: Number(this.form.maxLoadKg || 0),
      tankCapacityLiters: Number(this.form.tankCapacityLiters || 0),
      fuelConsumptionPerKm: Number(this.form.fuelConsumptionPerKm || 0),
      fuelType: this.form.fuelType,

      fuelLevelLiters: Number(this.selectedTruck.fuelLevelLiters || 0),
      currentLoadKg: Number(this.selectedTruck.currentLoadKg || 0),
      maxBinCapacity: this.selectedTruck.maxBinCapacity,
      lastKnownLat: this.selectedTruck.lastKnownLat,
      lastKnownLng: this.selectedTruck.lastKnownLng
    };

    this.truckService.update(this.selectedTruckId, payload).subscribe({
      next: () => {
        this.successMessage = 'Camion modifié avec succès.';
        this.closeForm();
        this.loadTrucks();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la modification du camion.';
      }
    });
  }

  applyFilters(): void {
    const term = this.searchTerm.toLowerCase().trim();

    this.filteredTrucks = this.trucks.filter((truck) =>
      !term ||
      truck.truckCode?.toLowerCase().includes(term) ||
      truck.plateNumber?.toLowerCase().includes(term) ||
      truck.assignedDriverName?.toLowerCase().includes(term)
    );
  }

  getDriverName(driver: DriverResponse): string {
    return driver.fullName || driver.username || driver.email || `Chauffeur ${driver.id}`;
  }

  getFuelPercent(truck: TruckResponse): number {
    const fuel = Number(truck.fuelLevelLiters || 0);
    const tank = Number(truck.tankCapacityLiters || 0);
    if (tank <= 0) return 0;
    return Math.min(100, Math.round((fuel * 100) / tank));
  }

  getStatusLabel(status?: string): string {
    switch (status) {
      case 'AVAILABLE': return 'Disponible';
      case 'ON_MISSION': return 'En mission';
      case 'BREAKDOWN': return 'En panne';
      case 'MAINTENANCE': return 'Maintenance';
      case 'REFUELING': return 'Ravitaillement';
      case 'UNAVAILABLE': return 'Indisponible';
      case 'OUT_OF_SERVICE': return 'Hors service';
      default: return 'Inconnu';
    }
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}