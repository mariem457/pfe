import { Component, OnInit } from '@angular/core';
import { TruckRequest, TruckResponse, TruckService, TruckStatus, FuelType } from '../../../../../services/truck.service';

@Component({
  selector: 'app-truck-management',
  templateUrl: './truck-management.component.html',
  styleUrls: ['./truck-management.component.css']
})
export class TruckManagementComponent implements OnInit {
  trucks: TruckResponse[] = [];
  filteredTrucks: TruckResponse[] = [];

  loading = false;
  errorMessage = '';
  successMessage = '';

  searchTerm = '';
  selectedStatus = 'ALL';
  selectedActive = 'ALL';

  showForm = false;
  editMode = false;
  selectedTruckId: number | null = null;

  statuses: TruckStatus[] = [
    'AVAILABLE',
    'ON_MISSION',
    'BREAKDOWN',
    'MAINTENANCE',
    'REFUELING',
    'UNAVAILABLE',
    'OUT_OF_SERVICE'
  ];

  fuelTypes: FuelType[] = ['DIESEL', 'ESSENCE', 'ELECTRIC', 'HYBRID'];

  form: TruckRequest = this.getEmptyForm();

  constructor(private truckService: TruckService) {}

  ngOnInit(): void {
    this.loadTrucks();
  }

  loadTrucks(): void {
    this.loading = true;
    this.errorMessage = '';

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

  getEmptyForm(): TruckRequest {
    return {
      truckCode: '',
      plateNumber: '',
      model: '',
      brand: '',
      fuelType: 'DIESEL',
      tankCapacityLiters: 100,
      fuelLevelLiters: 50,
      fuelConsumptionPerKm: 0.25,
      maxLoadKg: 1000,
      maxBinCapacity: 20,
      currentLoadKg: 0,
      status: 'AVAILABLE',
      lastKnownLat: undefined,
      lastKnownLng: undefined,
      isActive: true,
      assignedDriverId: null
    };
  }

  openCreateForm(): void {
    this.showForm = true;
    this.editMode = false;
    this.selectedTruckId = null;
    this.form = this.getEmptyForm();
    this.clearMessages();
  }

  openEditForm(truck: TruckResponse): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedTruckId = truck.id;

    this.form = {
      truckCode: truck.truckCode,
      plateNumber: truck.plateNumber || '',
      model: truck.model || '',
      brand: truck.brand || '',
      fuelType: truck.fuelType || 'DIESEL',
      tankCapacityLiters: Number(truck.tankCapacityLiters || 0),
      fuelLevelLiters: Number(truck.fuelLevelLiters || 0),
      fuelConsumptionPerKm: Number(truck.fuelConsumptionPerKm || 0),
      maxLoadKg: Number(truck.maxLoadKg || 0),
      maxBinCapacity: Number(truck.maxBinCapacity || 0),
      currentLoadKg: Number(truck.currentLoadKg || 0),
      status: truck.status || 'AVAILABLE',
      lastKnownLat: truck.lastKnownLat,
      lastKnownLng: truck.lastKnownLng,
      isActive: truck.isActive,
      assignedDriverId: truck.assignedDriverId || null
    };

    this.clearMessages();
  }

  closeForm(): void {
    this.showForm = false;
    this.editMode = false;
    this.selectedTruckId = null;
    this.form = this.getEmptyForm();
  }

  saveTruck(): void {
    this.clearMessages();

    if (!this.form.truckCode || !this.form.fuelType) {
      this.errorMessage = 'Code camion et type carburant sont obligatoires.';
      return;
    }

    if (this.editMode && this.selectedTruckId) {
      this.truckService.update(this.selectedTruckId, this.form).subscribe({
        next: () => {
          this.successMessage = 'Camion modifié avec succès.';
          this.closeForm();
          this.loadTrucks();
        },
        error: () => {
          this.errorMessage = 'Erreur lors de la modification du camion.';
        }
      });
    } else {
      this.truckService.create(this.form).subscribe({
        next: () => {
          this.successMessage = 'Camion ajouté avec succès.';
          this.closeForm();
          this.loadTrucks();
        },
        error: () => {
          this.errorMessage = 'Erreur lors de l’ajout du camion.';
        }
      });
    }
  }

  changeStatus(truck: TruckResponse, status: TruckStatus): void {
    this.truckService.updateStatus(truck.id, { status }).subscribe({
      next: () => {
        this.successMessage = 'Statut mis à jour.';
        this.loadTrucks();
      },
      error: () => {
        this.errorMessage = 'Impossible de changer le statut.';
      }
    });
  }

  deactivateTruck(truck: TruckResponse): void {
    const ok = confirm(`Désactiver le camion ${truck.truckCode} ?`);
    if (!ok) return;

    this.truckService.deactivate(truck.id).subscribe({
      next: () => {
        this.successMessage = 'Camion désactivé.';
        this.loadTrucks();
      },
      error: () => {
        this.errorMessage = 'Impossible de désactiver le camion.';
      }
    });
  }

  applyFilters(): void {
    const term = this.searchTerm.toLowerCase().trim();

    this.filteredTrucks = this.trucks.filter((truck) => {
      const matchesSearch =
        !term ||
        truck.truckCode?.toLowerCase().includes(term) ||
        truck.plateNumber?.toLowerCase().includes(term) ||
        truck.brand?.toLowerCase().includes(term) ||
        truck.model?.toLowerCase().includes(term) ||
        truck.assignedDriverName?.toLowerCase().includes(term);

      const matchesStatus =
        this.selectedStatus === 'ALL' || truck.status === this.selectedStatus;

      const matchesActive =
        this.selectedActive === 'ALL' ||
        (this.selectedActive === 'ACTIVE' && truck.isActive) ||
        (this.selectedActive === 'INACTIVE' && !truck.isActive);

      return matchesSearch && matchesStatus && matchesActive;
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = 'ALL';
    this.selectedActive = 'ALL';
    this.applyFilters();
  }

  get totalTrucks(): number {
    return this.trucks.length;
  }

  get availableTrucks(): number {
    return this.trucks.filter(t => t.status === 'AVAILABLE').length;
  }

  get missionTrucks(): number {
    return this.trucks.filter(t => t.status === 'ON_MISSION').length;
  }

  get problemTrucks(): number {
    return this.trucks.filter(t =>
      t.status === 'BREAKDOWN' ||
      t.status === 'MAINTENANCE' ||
      t.status === 'OUT_OF_SERVICE'
    ).length;
  }

  getFuelPercent(truck: TruckResponse): number {
    const fuel = Number(truck.fuelLevelLiters || 0);
    const tank = Number(truck.tankCapacityLiters || 0);
    if (!tank) return 0;
    return Math.round((fuel * 100) / tank);
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

  getStatusClass(status?: string): string {
    switch (status) {
      case 'AVAILABLE': return 'status available';
      case 'ON_MISSION': return 'status mission';
      case 'BREAKDOWN': return 'status breakdown';
      case 'MAINTENANCE': return 'status maintenance';
      case 'REFUELING': return 'status refueling';
      case 'UNAVAILABLE': return 'status unavailable';
      case 'OUT_OF_SERVICE': return 'status out';
      default: return 'status';
    }
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}