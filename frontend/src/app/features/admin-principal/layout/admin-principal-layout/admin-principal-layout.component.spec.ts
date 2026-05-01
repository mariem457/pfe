import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminPrincipalLayoutComponent } from './admin-principal-layout.component';

describe('LayoutComponent', () => {
  let component: AdminPrincipalLayoutComponent;
  let fixture: ComponentFixture<AdminPrincipalLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminPrincipalLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminPrincipalLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
