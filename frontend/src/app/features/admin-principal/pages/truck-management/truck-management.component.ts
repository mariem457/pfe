import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TruckRequest, TruckResponse, TruckService, TruckStatus, FuelType, ZoneResponse, WasteType } from '../../../../services/truck.service';

type WasteTypeChoice = WasteType | 'ALL';

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
  zoneOptions: Array<Partial<ZoneResponse> & { name: string }> = [];
  private readonly paris15ZoneNames = [
    'Saint-Lambert',
    'Necker',
    'Grenelle',
    'Javel'
  ];

  loading = false;
  saving = false;
  errorMessage = '';
  successMessage = '';

  searchTerm = '';
  selectedStatus = 'ALL';

  showForm = false;
  editMode = false;
  selectedTruckId: number | null = null;
  openedActionTruckId: number | null = null;

  statuses: TruckStatus[] = [
    'AVAILABLE',
    'BREAKDOWN',
    'MAINTENANCE'
  ];

  fuelTypes: FuelType[] = ['DIESEL', 'ESSENCE', 'ELECTRIC', 'HYBRID'];
  wasteTypeOptions: Array<{ value: WasteTypeChoice; label: string }> = [
    { value: 'ALL', label: 'Tous les types' },
    { value: 'GRAY', label: 'Déchets ménagers' },
    { value: 'GREEN', label: 'Verre' },
    { value: 'YELLOW', label: 'Recyclable' },
    { value: 'WHITE', label: 'Papier' }
  ];
  selectedWasteType: WasteTypeChoice = 'ALL';

  form: TruckRequest = this.getEmptyForm();

  constructor(private truckService: TruckService) {}

  ngOnInit(): void {
    this.zoneOptions = this.paris15ZoneNames.map((name) => ({ name }));
    this.loadZones();
    this.loadTrucks();
  }

  loadZones(): void {
    this.truckService.getZones().subscribe({
      next: (zones) => {
        this.zoneOptions = this.paris15ZoneNames
          .map((name) => (zones || []).find((zone) => zone.name === name) || { name });
      },
      error: () => {
        this.zoneOptions = this.paris15ZoneNames.map((name) => ({ name }));
      }
    });
  }

  loadTrucks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.truckService.getAll().subscribe({
      next: (data) => {
        this.trucks = (data || []).filter((truck) => truck.isActive !== false);
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
      zoneId: null,
      zoneName: null,
      isActive: true,
      assignedDriverId: null
    };
  }

  openCreateForm(): void {
    this.showForm = true;
    this.editMode = false;
    this.selectedTruckId = null;
    this.form = this.getEmptyForm();
    this.selectedWasteType = 'ALL';
    this.clearMessages();
  }

  openEditForm(truck: TruckResponse): void {
    this.closeActionMenu();
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
      zoneId: truck.zoneId || null,
      zoneName: truck.zoneName || null,
      isActive: truck.isActive,
      assignedDriverId: truck.assignedDriverId || null,
      supportedWasteTypes: truck.supportedWasteTypes || []
    };
    this.selectedWasteType = this.resolveWasteTypeChoice(truck.supportedWasteTypes);

    this.clearMessages();
  }

  closeForm(): void {
    this.showForm = false;
    this.editMode = false;
    this.selectedTruckId = null;
    this.form = this.getEmptyForm();
    this.selectedWasteType = 'ALL';
    this.saving = false;
  }

  saveTruck(): void {
    this.clearMessages();

    const payload = this.buildPayload();

    if (!payload.truckCode || !payload.fuelType) {
      this.errorMessage = 'Code camion et type carburant sont obligatoires.';
      return;
    }

    this.saving = true;

    if (this.editMode && this.selectedTruckId) {
      this.truckService.update(this.selectedTruckId, payload).subscribe({
        next: () => {
          this.successMessage = 'Camion modifié avec succès.';
          this.closeForm();
          this.loadTrucks();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getBackendErrorMessage(error, 'Erreur lors de la modification du camion.');
          this.saving = false;
        }
      });
    } else {
      this.truckService.create(payload).subscribe({
        next: () => {
          this.successMessage = 'Camion ajouté avec succès.';
          this.closeForm();
          this.loadTrucks();
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.getBackendErrorMessage(error, 'Erreur lors de l ajout du camion.');
          this.saving = false;
        }
      });
    }
  }

  private buildPayload(): TruckRequest {
    return {
      truckCode: this.cleanText(this.form.truckCode) || '',
      plateNumber: this.cleanText(this.form.plateNumber),
      model: this.cleanText(this.form.model),
      brand: this.cleanText(this.form.brand),
      fuelType: this.form.fuelType || 'DIESEL',
      tankCapacityLiters: this.toNumberOrUndefined(this.form.tankCapacityLiters),
      fuelLevelLiters: this.toNumberOrUndefined(this.form.fuelLevelLiters),
      fuelConsumptionPerKm: this.toNumberOrUndefined(this.form.fuelConsumptionPerKm),
      maxLoadKg: this.toNumberOrUndefined(this.form.maxLoadKg),
      maxBinCapacity: this.toNumberOrUndefined(this.form.maxBinCapacity),
      currentLoadKg: this.toNumberOrUndefined(this.form.currentLoadKg),
      status: this.form.status || 'AVAILABLE',
      zoneId: this.toNumberOrNull(this.form.zoneId),
      zoneName: this.cleanText(this.form.zoneName),
      isActive: this.form.isActive ?? true,
      assignedDriverId: this.toNumberOrNull(this.form.assignedDriverId),
      supportedWasteTypes: this.resolveSupportedWasteTypes(this.selectedWasteType)
    };
  }

  private resolveSupportedWasteTypes(choice: WasteTypeChoice): WasteType[] {
    if (choice === 'ALL') {
      return ['GRAY', 'GREEN', 'YELLOW', 'WHITE'];
    }

    return [choice];
  }

  private resolveWasteTypeChoice(types?: WasteType[]): WasteTypeChoice {
    const normalizedTypes = new Set((types || []).filter(Boolean));
    const allTypes: WasteType[] = ['GRAY', 'GREEN', 'YELLOW', 'WHITE'];

    if (allTypes.every((type) => normalizedTypes.has(type))) {
      return 'ALL';
    }

    return allTypes.find((type) => normalizedTypes.has(type)) || 'ALL';
  }

  private cleanText(value: unknown): string | undefined {
    if (typeof value !== 'string') {
      return undefined;
    }

    const trimmed = value.trim();
    return trimmed || undefined;
  }

  private toNumberOrUndefined(value: unknown): number | undefined {
    if (value === null || value === undefined || value === '') {
      return undefined;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private toNumberOrNull(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }

    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private getBackendErrorMessage(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (error.error?.message) {
      return error.error.message;
    }

    if (error.error?.errors) {
      return Object.values(error.error.errors).join(' ');
    }

    return fallback;
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

  deleteTruck(truck: TruckResponse): void {
    this.closeActionMenu();

    const ok = confirm(`Supprimer le camion ${truck.truckCode} ?`);
    if (!ok) return;

    this.truckService.deactivate(truck.id).subscribe({
      next: () => {
        this.successMessage = 'Camion supprimÃ©.';
        this.loadTrucks();
      },
      error: () => {
        this.errorMessage = 'Impossible de supprimer le camion.';
      }
    });
  }

  toggleActionMenu(truckId: number): void {
    this.openedActionTruckId = this.openedActionTruckId === truckId ? null : truckId;
  }

  closeActionMenu(): void {
    this.openedActionTruckId = null;
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

      return matchesSearch && matchesStatus;
    });
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedStatus = 'ALL';
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
      t.status === 'MAINTENANCE'
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

  getWasteTypeLabel(types?: WasteType[]): string {
    return this.wasteTypeOptions.find((option) => option.value === this.resolveWasteTypeChoice(types))?.label || '-';
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
