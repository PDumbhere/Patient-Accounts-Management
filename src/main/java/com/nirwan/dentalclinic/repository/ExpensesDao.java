package com.nirwan.dentalclinic.repository;

import com.nirwan.dentalclinic.database.DatabaseConnection;
import com.nirwan.dentalclinic.models.Expenses;
import com.nirwan.dentalclinic.models.PaymentReportRow;
import javafx.scene.control.Alert;
import lombok.NoArgsConstructor;

import java.sql.*;
import java.time.LocalDateTime;

@NoArgsConstructor
public class ExpensesDao {

    private static final String INSERT_QUERY = """
            insert into Expenses (type, product, name, amount, transaction_date)
            values
            (?,?,?,?,?);
            """;

    private static final String UPDATE_QUERY = """
            update Expenses set type = ?, product = ?, name = ?, amount = ?,
            transaction_date = ? where id = ?
            """;

    private static final String DELETE_QUERY = """
            update Expenses set is_deleted = true where id = ?
            """;

    public boolean saveExpense(Expenses expenses) throws SQLException{
        try(Connection connection = DatabaseConnection.getInstance().getConnection();
            PreparedStatement stmt = connection.prepareStatement(INSERT_QUERY)){
                stmt.setString(1, expenses.getType());
                stmt.setString(2,expenses.getProduct());
                stmt.setString(3, expenses.getName());
                stmt.setDouble(4, expenses.getAmount());
                stmt.setDate(5, Date.valueOf(expenses.getPaymentDate()));

                return stmt.executeUpdate() != 0;
        } catch (SQLException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("SQL Exception");
            alert.setHeaderText("Unable to save expense record");
            alert.setContentText(e.getMessage());
            alert.show();
            e.printStackTrace();
            return false;
        } catch (Exception e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Unable to save expense record");
            alert.setContentText(e.getMessage());
            alert.show();
            e.printStackTrace();
            return false;
        }
    }

    public boolean editExpense(Expenses updatedExpense){
        try(Connection connection = DatabaseConnection.getInstance().getConnection();
        PreparedStatement stmt = connection.prepareStatement(UPDATE_QUERY)){
            stmt.setString(1,updatedExpense.getType());
            stmt.setString(2, updatedExpense.getProduct());
            stmt.setString(3, updatedExpense.getName());
            stmt.setDouble(4, updatedExpense.getAmount());
            stmt.setDate(5, Date.valueOf(updatedExpense.getPaymentDate()));
            stmt.setInt(6, updatedExpense.getId());

            return stmt.executeUpdate() != 0;
        } catch (SQLException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("SQL Exception");
            alert.setHeaderText("Unable to Edit expense record");
            alert.setContentText(e.getMessage());
            alert.show();
            e.printStackTrace();
            return false;
        } catch (Exception e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Unable to Edit expense record");
            alert.setContentText(e.getMessage());
            alert.show();
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteExpense(int expenseId){
        try(Connection connection = DatabaseConnection.getInstance().getConnection();
        PreparedStatement stmt = connection.prepareStatement(DELETE_QUERY)){
            stmt.setInt(1, expenseId);
            return stmt.executeUpdate() != 0;
        } catch (SQLException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("SQL Exception");
            alert.setHeaderText("Unable to Delete expense record");
            alert.setContentText(e.getMessage());
            alert.show();
            e.printStackTrace();
            return false;
        } catch (Exception e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Unable to Delete expense record");
            alert.setContentText(e.getMessage());
            alert.show();
            e.printStackTrace();
            return false;
        }
    }
}
