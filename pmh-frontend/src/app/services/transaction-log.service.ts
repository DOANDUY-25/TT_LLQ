import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { TransactionLogSearchDTO, TransactionLogSearchResponse } from '../models/transaction-log.model';

@Injectable({
  providedIn: 'root'
})
export class TransactionLogService {
  private apiUrl = `${environment.apiUrl}/api/transaction-logs`;

  constructor(private http: HttpClient) {}

  search(
    searchDTO: TransactionLogSearchDTO,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'createdAt',
    sortDir: string = 'DESC'
  ): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.post<any>(`${this.apiUrl}/search`, searchDTO, { params });
  }

  exportFile(searchDTO: TransactionLogSearchDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/export`, searchDTO);
  }
}
