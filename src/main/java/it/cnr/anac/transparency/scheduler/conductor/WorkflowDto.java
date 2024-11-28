/*
 * Copyright (C) 2024 Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package it.cnr.anac.transparency.scheduler.conductor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

@Data
public class WorkflowDto {

  @JsonDeserialize(using = LocalDateTimeFromEpochDeserializer.class)
  private LocalDateTime createTime; //": 1726055743122,
  @JsonDeserialize(using = LocalDateTimeFromEpochDeserializer.class)
  private LocalDateTime updateTime; //": 1726126135420,
  private String status; //: "COMPLETED",
  @JsonDeserialize(using = LocalDateTimeFromEpochDeserializer.class)
  private LocalDateTime endTime; //: 1726126135420,
  private String workflowId; //: "7eab673f-0d31-4bfc-b2b0-731558ecc32f",
}
