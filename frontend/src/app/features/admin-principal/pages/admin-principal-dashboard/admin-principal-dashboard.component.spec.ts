import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminPrincipalDashboardComponent } from './admin-principal-dashboard.component';

describe('AdminPrincipalDashboardComponent', () => {
  let component: AdminPrincipalDashboardComponent;
  let fixture: ComponentFixture<AdminPrincipalDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPrincipalDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminPrincipalDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
