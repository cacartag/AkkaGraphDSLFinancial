package models;

import java.util.List;

public record Transaction(List<Settlement> settlements,
                          List<Tender> tenders ) { }
