import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ControleSystemeComponent } from './controle-systeme.component';

describe('ControleSystemeComponent', () => {
  let component: ControleSystemeComponent;
  let fixture: ComponentFixture<ControleSystemeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ControleSystemeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ControleSystemeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
