import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionPoubellesComponent } from './gestion-poubelles.component';

describe('GestionPoubellesComponent', () => {
  let component: GestionPoubellesComponent;
  let fixture: ComponentFixture<GestionPoubellesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionPoubellesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GestionPoubellesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
