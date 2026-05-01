import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChauffeurDashboardComponent } from './chauffeur-dashboard.component';

describe('DashboardComponent', () => {
  let component: ChauffeurDashboardComponent;
  let fixture: ComponentFixture<ChauffeurDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChauffeurDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChauffeurDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
